package lightning.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lightning.enums.CacheControl;
import lightning.http.NotFoundException;
import lightning.util.Mimes;
import lightning.util.Time;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.http.DateParser;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.http.ResourceHttpContent;
import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.InclusiveByteRange;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ResourceCache;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.MultiPartOutputStream;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles serving static files either individually or from a directory.
 * Supports the HTTP protocol fully (caching, partials).
 * Supports fast serving of a directory using file mapped buffer caching.
 * NOTE: This code is an adaptation of Jetty's default servlet code to work with this framework.
 * NOTE: HTTP caching in chrome doesn't work properly w/ HTTPS self-signed certificates?
 * TODO(mschurr): On-the-fly GZIP support?
 * TODO(mschurr): This is pretty convoluted, should just rewrite it from scratch time allowing.
 */
public class FileServer implements ResourceFactory {
  private static final Logger logger = LoggerFactory.getLogger(FileServer.class);
  private static final PreEncodedHttpField ACCEPT_RANGES = new PreEncodedHttpField(
      HttpHeader.ACCEPT_RANGES, "bytes");

  private boolean caching = true;
  private boolean _acceptRanges = true;
  private boolean _gzip = true;
  private boolean _pathInfoOnly = true;
  private boolean _etags = true;
  private boolean disableAsync = false;
  private int maxCacheSize = 1024 * 1024 * 30;
  private int maxCachedFiles = 500;
  private int maxCachedFileSize = 1024 * 1024 * 1;

  private ResourceCache _cache;

  private boolean _useFileMappedBuffer = true;
  private PreEncodedHttpField _defaultCacheControl = null;
  private List<String> _gzipEquivalentFileExtensions;
  private ResourceFactory factory;
  
  public FileServer(ResourceFactory factory) throws Exception {
    this.factory = factory;
    
    _cache = new ResourceCache(null, this, new MimeTypes(), _useFileMappedBuffer, _etags, _gzip);

    if (maxCacheSize >= 0)
      _cache.setMaxCacheSize(maxCacheSize);
    if (maxCachedFileSize >= -1)
      _cache.setMaxCachedFileSize(maxCachedFileSize);
    if (maxCachedFiles >= -1)
      _cache.setMaxCachedFiles(maxCachedFiles);      

    _gzipEquivalentFileExtensions = new ArrayList<String>();
    _gzipEquivalentFileExtensions.add(".svgz");
    usePublicCaching();
  }
  
  public void usePrivateCaching() {
    _defaultCacheControl = getCacheControl(CacheControl.PRIVATE);
  }
  
  public void usePublicCaching() {
    _defaultCacheControl = getCacheControl(CacheControl.PUBLIC);
  }
  
  public void setMaxCacheSize(int value) {
    this.maxCacheSize = value;
  }
  
  public void setMaxCachedFiles(int value) {
    this.maxCachedFiles = value;
  }
  
  public void setMaxCachedFileSize(int value) {
    this.maxCachedFileSize = value;
  }
  
  public void disableAsync() {
    this.disableAsync = true; // Enables closing the IO stream on completion.
  }
  
  public void disableCaching() {
    _defaultCacheControl = getCacheControl(CacheControl.NO_CACHE);
    _etags = false;
    _cache.flushCache();
    _cache = null;
    _useFileMappedBuffer = false;
    caching = false;
  }


  /* ------------------------------------------------------------ */
  /**
   * get Resource to serve. Map a path to a resource. The default implementation calls
   * HttpContext.getResource but derived servlets may provide their own mapping.
   * 
   * @param pathInContext The path to find a resource for.
   * @return The resource to serve.
   */
  @Override
  public Resource getResource(String pathInContext) {
    return factory.getResource(pathInContext);
    /*Resource r = null;
    if (_relativeResourceBase != null)
      pathInContext = URIUtil.addPaths(_relativeResourceBase, pathInContext);

    try {
      if (_resourceBase != null) {
        r = _resourceBase.addPath(pathInContext);
        if (!_contextHandler.checkAlias(pathInContext, r))
          r = null;
      } else {
        URL u = _servletContext.getResource(pathInContext);
        r = _contextHandler.newResource(u);
      }

      logger.debug("Resource " + pathInContext + "=" + r);
    } catch (IOException e) {
    }

    return r;*/
  }
  
  public boolean couldConsume(HttpServletRequest request) {
    String path = getPathInContext(request);
    
    try {
      if (_cache != null) {
        HttpContent content = _cache.getContent(path); //_cache.lookup(path);
        
        if (content != null &&
            !content.getResource().isDirectory()) {
          return true;
        }
      }
    } catch (IOException e) {}
    
    Resource resource = getResource(path);
    return resource != null && resource.exists() && !resource.isDirectory();
  }
  
  public String getPathInContext(HttpServletRequest request) {
    String servletPath = null;
    String pathInfo = null;
    Enumeration<String> reqRanges = null;
    Boolean included = request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
    if (included != null && included.booleanValue()) {
      servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
      pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
      if (servletPath == null) {
        servletPath = request.getServletPath();
        pathInfo = request.getPathInfo();
      }
    } else {
      included = Boolean.FALSE;
      servletPath = _pathInfoOnly ? "/" : request.getServletPath();
      pathInfo = request.getPathInfo();

      // Is this a Range request?
      reqRanges = request.getHeaders(HttpHeader.RANGE.asString());
      if (!hasDefinedRange(reqRanges))
        reqRanges = null;
    }

    String pathInContext = URIUtil.addPaths(servletPath, pathInfo);
    return pathInContext;
  }
  
  protected PreEncodedHttpField getCacheControl(CacheControl type) {
    switch (type) {
      case NO_CACHE:
        return new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
      case PRIVATE:
        return new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "private, max-age=3600");
      default:
      case PUBLIC:
        return new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "public, max-age=3600");
    }
  }

  /* ------------------------------------------------------------ */
  
  // NOTE: Doesn't use resource cache or memory mapping, but still supports full spec.
  public void sendResource(HttpServletRequest request, HttpServletResponse response, Resource resource, CacheControl cacheType) throws IOException {
    PreEncodedHttpField cacheControl = getCacheControl(cacheType);
    
    if (resource == null || !resource.exists()) {
      throw new NotFoundException();
    }
    
    HttpContent content = null;
    boolean close_content = true;
    Enumeration<String> reqRanges = null;
    
    //response.addHeader("Content-Disposition", "inline, filename=" + FilenameUtils.getBaseName(resource.getName()));
    
    try {
      reqRanges = request.getHeaders(HttpHeader.RANGE.asString());
      if (!hasDefinedRange(reqRanges))
        reqRanges = null;
      
      content =
          new ResourceHttpContent(resource,
              Mimes.forPath(resource.toString()), response.getBufferSize());
      
      if (passConditionalHeaders(request, response, resource, content)) {
        close_content = sendData(request, response, false, resource, content, reqRanges, cacheControl);
      }
    } finally {
      if (close_content  && content != null) {
        content.release();
      }
    }
  }
  
  @SuppressWarnings("resource")
  public void handle(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String servletPath = null;
    String pathInfo = null;
    Enumeration<String> reqRanges = null;
    Boolean included = request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
    if (included != null && included.booleanValue()) {
      servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
      pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
      if (servletPath == null) {
        servletPath = request.getServletPath();
        pathInfo = request.getPathInfo();
      }
    } else {
      included = Boolean.FALSE;
      servletPath = _pathInfoOnly ? "/" : request.getServletPath();
      pathInfo = request.getPathInfo();

      // Is this a Range request?
      reqRanges = request.getHeaders(HttpHeader.RANGE.asString());
      if (!hasDefinedRange(reqRanges))
        reqRanges = null;
    }

    String pathInContext = URIUtil.addPaths(servletPath, pathInfo);
    boolean endsWithSlash =
        (pathInfo == null ? request.getServletPath() : pathInfo).endsWith(URIUtil.SLASH);


    // Find the resource and content
    Resource resource = null;
    HttpContent content = null;
    boolean close_content = true;
    try {
      // is gzip enabled?
      String pathInContextGz = null;
      boolean gzip = false;
      if (!included.booleanValue() && _gzip && reqRanges == null && !endsWithSlash) {
        // Look for a gzip resource
        pathInContextGz = pathInContext + ".gz";
        if (_cache == null)
          resource = getResource(pathInContextGz);
        else {
          content = _cache.getContent(pathInContextGz); // _cache.lookup(pathInContextGz);
          resource = (content == null) ? null : content.getResource();
        }

        // Does a gzip resource exist?
        if (resource != null && resource.exists() && !resource.isDirectory()) {
          // Tell caches that response may vary by accept-encoding
          response.addHeader(HttpHeader.VARY.asString(), HttpHeader.ACCEPT_ENCODING.asString());

          // Does the client accept gzip?
          String accept = request.getHeader(HttpHeader.ACCEPT_ENCODING.asString());
          if (accept != null && accept.indexOf("gzip") >= 0)
            gzip = true;
        }
      }

      // find resource
      if (!gzip) {
        if (_cache == null)
          resource = getResource(pathInContext);
        else {
          content = _cache.getContent(pathInContext); //_cache.lookup(pathInContext);
          resource = content == null ? null : content.getResource();
        }
      }

      logger.debug(String.format("uri=%s, resource=%s, content=%s", request.getRequestURI(),
          resource, content));

      // Handle resource
      if (resource == null || !resource.exists()) {
        if (included)
          throw new FileNotFoundException("!" + pathInContext);
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } else if (!resource.isDirectory()) {
        if (endsWithSlash && pathInContext.length() > 1) {
          String q = request.getQueryString();
          pathInContext = pathInContext.substring(0, pathInContext.length() - 1);
          if (q != null && q.length() != 0)
            pathInContext += "?" + q;
          response.sendRedirect(response.encodeRedirectURL(pathInContext));
        } else {
          // ensure we have content
          if (content == null)
            content =
                new ResourceHttpContent(resource,
                    Mimes.forPath(resource.toString()), response.getBufferSize());

          if (included.booleanValue()
              || passConditionalHeaders(request, response, resource, content)) {
            if (gzip || isGzippedContent(pathInContext)) {
              response.setHeader(HttpHeader.CONTENT_ENCODING.asString(), "gzip");
              String mt = Mimes.forExtension(FilenameUtils.getExtension(pathInContext));
              if (mt != null)
                response.setContentType(mt);
            }
            close_content =
                sendData(request, response, included.booleanValue(), resource, content, reqRanges, _defaultCacheControl);
          }
        }
      } else {
        if (!endsWithSlash
            || (pathInContext.length() == 1 && request
                .getAttribute("org.eclipse.jetty.server.nullPathInfo") != null)) {
          StringBuffer buf = request.getRequestURL();
          synchronized (buf) {
            int param = buf.lastIndexOf(";");
            if (param < 0)
              buf.append('/');
            else
              buf.insert(param, '/');
            String q = request.getQueryString();
            if (q != null && q.length() != 0) {
              buf.append('?');
              buf.append(q);
            }
            response.setContentLength(0);
            response.sendRedirect(response.encodeRedirectURL(buf.toString()));
          }
        } else {
          content =
              new ResourceHttpContent(resource, Mimes.forPath(resource.toString()));
          if (included.booleanValue()
              || passConditionalHeaders(request, response, resource, content))
            sendDirectory(request, response, resource, pathInContext);
        }
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Got bad URI while trying to server static files: ", e);
      if (!response.isCommitted())
        response.sendError(500, e.getMessage());
    } finally {
      if (close_content) {
        if (content != null)
          content.release();
        else if (resource != null)
          resource.close();
      }
    }

  }

  private void sendDirectory(HttpServletRequest request, HttpServletResponse response,
      Resource resource, String pathInContext) {}

  protected boolean isGzippedContent(String path) {
    if (path == null)
      return false;

    for (String suffix : _gzipEquivalentFileExtensions)
      if (path.endsWith(suffix))
        return true;
    return false;
  }

  /* ------------------------------------------------------------ */
  private boolean hasDefinedRange(Enumeration<String> reqRanges) {
    return (reqRanges != null && reqRanges.hasMoreElements());
  }

  /* ------------------------------------------------------------ */
  /*
   * Check modification date headers.
   */
  protected boolean passConditionalHeaders(HttpServletRequest request,
      HttpServletResponse response, Resource resource, HttpContent content) throws IOException {
    try {
      String ifm = null;
      String ifnm = null;
      String ifms = null;
      long ifums = -1;

      if (request instanceof Request) {
        HttpFields fields = ((Request) request).getHttpFields();
        for (int i = fields.size(); i-- > 0;) {
          HttpField field = fields.getField(i);
          if (field.getHeader() != null) {
            switch (field.getHeader()) {
              case IF_MATCH:
                ifm = field.getValue();
                break;
              case IF_NONE_MATCH:
                ifnm = field.getValue();
                break;
              case IF_MODIFIED_SINCE:
                ifms = field.getValue();
                break;
              case IF_UNMODIFIED_SINCE:
                ifums = DateParser.parseDate(field.getValue());
                break;
              default:
            }
          }
        }
      } else {
        ifm = request.getHeader(HttpHeader.IF_MATCH.asString());
        ifnm = request.getHeader(HttpHeader.IF_NONE_MATCH.asString());
        ifms = request.getHeader(HttpHeader.IF_MODIFIED_SINCE.asString());
        ifums = request.getDateHeader(HttpHeader.IF_UNMODIFIED_SINCE.asString());
      }

      if (!HttpMethod.HEAD.is(request.getMethod())) {
        if (_etags) {
          if (ifm != null) {
            boolean match = false;
            if (content.getETagValue() != null) {
              QuotedStringTokenizer quoted = new QuotedStringTokenizer(ifm, ", ", false, true);
              while (!match && quoted.hasMoreTokens()) {
                String tag = quoted.nextToken();
                if (content.getETagValue().equals(tag))
                  match = true;
              }
            }

            if (!match) {
              response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
              return false;
            }
          }

          if (ifnm != null && content.getETagValue() != null) {
            // Look for Gzip'd version of etag
            if (content.getETagValue().equals(request.getAttribute("o.e.j.s.Gzip.ETag"))) {
              response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
              response.setHeader(HttpHeader.ETAG.asString(), ifnm);
              return false;
            }

            // Handle special case of exact match.
            if (content.getETagValue().equals(ifnm)) {
              response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
              response.setHeader(HttpHeader.ETAG.asString(), content.getETagValue());
              return false;
            }

            // Handle list of tags
            QuotedStringTokenizer quoted = new QuotedStringTokenizer(ifnm, ", ", false, true);
            while (quoted.hasMoreTokens()) {
              String tag = quoted.nextToken();
              if (content.getETagValue().equals(tag)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.setHeader(HttpHeader.ETAG.asString(), content.getETagValue());
                return false;
              }
            }

            // If etag requires content to be served, then do not check if-modified-since
            return true;
          }
        }

        // Handle if modified since
        if (ifms != null) {
          // Get jetty's Response impl
          String mdlm = content.getLastModifiedValue();
          if (mdlm != null && ifms.equals(mdlm)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            if (_etags)
              response.setHeader(HttpHeader.ETAG.asString(), content.getETagValue());
            response.flushBuffer();
            return false;
          }

          long ifmsl = request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.asString());
          if (ifmsl != -1 && resource.lastModified() / 1000 <= ifmsl / 1000) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            if (_etags)
              response.setHeader(HttpHeader.ETAG.asString(), content.getETagValue());
            response.flushBuffer();
            return false;
          }
        }

        // Parse the if[un]modified dates and compare to resource
        if (ifums != -1 && resource.lastModified() / 1000 > ifums / 1000) {
          response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
          return false;
        }

      }
    } catch (IllegalArgumentException iae) {
      if (!response.isCommitted())
        response.sendError(400, iae.getMessage());
      throw iae;
    }
    return true;
  }


  /* ------------------------------------------------------------ */
  protected boolean sendData(HttpServletRequest request, HttpServletResponse response,
      boolean include, Resource resource, final HttpContent content, Enumeration<String> reqRanges,
      PreEncodedHttpField cacheControl)
      throws IOException {
    final long content_length =
        (content == null) ? resource.length() : content.getContentLengthValue();

    // Get the output stream (or writer)
    OutputStream out = null;
    boolean written;
    try {
      out = response.getOutputStream();

      // has a filter already written to the response?
      written = out instanceof HttpOutput ? ((HttpOutput) out).isWritten() : true;
    } catch (IllegalStateException e) {
      out = new WriterOutputStream(response.getWriter());
      written = true; // there may be data in writer buffer, so assume written
    }

    logger.debug(String.format("sendData content=%s out=%s async=%b", content, out,
        request.isAsyncSupported()));

    if (reqRanges == null || !reqRanges.hasMoreElements() || content_length < 0) {
      // if there were no ranges, send entire entity
      if (include) {
        // write without headers
        resource.writeTo(out, 0, content_length);
      }
      // else if we can't do a bypass write because of wrapping
      else if (content == null || written || !(out instanceof HttpOutput)) {
        // write normally
        putHeaders(response, content, written ? -1 : 0, cacheControl);
        ByteBuffer buffer = (content == null) ? null : content.getIndirectBuffer();
        if (buffer != null)
          BufferUtil.writeTo(buffer, out);
        else
          resource.writeTo(out, 0, content_length);
      }
      // else do a bypass write
      else {
        // write the headers
        putHeaders(response, content, 0, cacheControl);

        // write the content asynchronously if supported
        if (request.isAsyncSupported() && !disableAsync) {
          final AsyncContext context = request.startAsync();
          logger.debug("Writing content asynchronously");
          context.setTimeout(0);

          ((HttpOutput) out).sendContent(content, new Callback() {
            @Override
            public void succeeded() {
              context.complete();
              content.release();
            }

            @Override
            public void failed(Throwable x) {
              if (x instanceof IOException)
                logger.debug("", x);
              else
                logger.warn("", x);
              context.complete();
              content.release();
            }

            @Override
            public String toString() {
              return "FileServer";
            }
          });
          return false;
        }
        // otherwise write content blocking
        logger.debug("Writing content synchronously");
        ((HttpOutput) out).sendContent(content);

      }
    } else {
      // Parse the satisfiable ranges
      List<InclusiveByteRange> ranges =
          InclusiveByteRange.satisfiableRanges(reqRanges, content_length);

      // if there are no satisfiable ranges, send 416 response
      if (ranges == null || ranges.size() == 0) {
        putHeaders(response, content, 0, cacheControl);
        response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setHeader(HttpHeader.CONTENT_RANGE.asString(),
            InclusiveByteRange.to416HeaderRangeString(content_length));
        resource.writeTo(out, 0, content_length);
        return true;
      }

      // if there is only a single valid range (must be satisfiable
      // since were here now), send that range with a 216 response
      if (ranges.size() == 1) {
        InclusiveByteRange singleSatisfiableRange = ranges.get(0);
        long singleLength = singleSatisfiableRange.getSize(content_length);
        putHeaders(response, content, singleLength, cacheControl);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        if (!response.containsHeader(HttpHeader.DATE.asString()))
          response.addDateHeader(HttpHeader.DATE.asString(), System.currentTimeMillis());
        response.setHeader(HttpHeader.CONTENT_RANGE.asString(),
            singleSatisfiableRange.toHeaderRangeString(content_length));
        resource.writeTo(out, singleSatisfiableRange.getFirst(content_length), singleLength);
        return true;
      }

      // multiple non-overlapping valid ranges cause a multipart
      // 216 response which does not require an overall
      // content-length header
      //
      putHeaders(response, content, -1, cacheControl);
      String mimetype = (content == null ? null : content.getContentTypeValue());
      if (mimetype == null)
        logger.warn("Unknown mimetype for " + request.getRequestURI());
      MultiPartOutputStream multi = new MultiPartOutputStream(out);
      response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
      if (!response.containsHeader(HttpHeader.DATE.asString()))
        response.addDateHeader(HttpHeader.DATE.asString(), System.currentTimeMillis());

      // If the request has a "Request-Range" header then we need to
      // send an old style multipart/x-byteranges Content-Type. This
      // keeps Netscape and acrobat happy. This is what Apache does.
      String ctp;
      if (request.getHeader(HttpHeader.REQUEST_RANGE.asString()) != null)
        ctp = "multipart/x-byteranges; boundary=";
      else
        ctp = "multipart/byteranges; boundary=";
      response.setContentType(ctp + multi.getBoundary());

      InputStream in = resource.getInputStream();
      long pos = 0;

      // calculate the content-length
      int length = 0;
      String[] header = new String[ranges.size()];
      for (int i = 0; i < ranges.size(); i++) {
        InclusiveByteRange ibr = ranges.get(i);
        header[i] = ibr.toHeaderRangeString(content_length);
        length +=
            ((i > 0) ? 2 : 0)
                + 2
                + multi.getBoundary().length()
                + 2
                + (mimetype == null ? 0 : HttpHeader.CONTENT_TYPE.asString().length() + 2
                    + mimetype.length()) + 2 + HttpHeader.CONTENT_RANGE.asString().length() + 2
                + header[i].length() + 2 + 2
                + (ibr.getLast(content_length) - ibr.getFirst(content_length)) + 1;
      }
      length += 2 + 2 + multi.getBoundary().length() + 2 + 2;
      response.setContentLength(length);

      for (int i = 0; i < ranges.size(); i++) {
        InclusiveByteRange ibr = ranges.get(i);
        multi.startPart(mimetype, new String[] {HttpHeader.CONTENT_RANGE + ": " + header[i]});

        long start = ibr.getFirst(content_length);
        long size = ibr.getSize(content_length);
        if (in != null) {
          // Handle non cached resource
          if (start < pos) {
            in.close();
            in = resource.getInputStream();
            pos = 0;
          }
          if (pos < start) {
            in.skip(start - pos);
            pos = start;
          }

          IO.copy(in, multi, size);
          pos += size;
        } else
          // Handle cached resource
          (resource).writeTo(multi, start, size);
      }
      if (in != null)
        in.close();
      multi.close();
    }
    return true;
  }

  protected void putHeaders(HttpServletResponse response, HttpContent content, long contentLength, PreEncodedHttpField cacheControl) {
    if (response instanceof Response) {
      Response r = (Response) response;
      r.putHeaders(content, contentLength, _etags);
      HttpFields f = r.getHttpFields();
      if (_acceptRanges)
        f.put(ACCEPT_RANGES);

      if (cacheControl != null)
        f.put(cacheControl);
    } else {
      Response.putHeaders(response, content, contentLength, _etags);
      if (_acceptRanges)
        response.setHeader(ACCEPT_RANGES.getName(), ACCEPT_RANGES.getValue());

      if (cacheControl != null)
        response.setHeader(cacheControl.getName(), cacheControl.getValue());
    }
    
    if (caching) {
      response.setHeader("Expires", Time.formatForHttp(Time.now() + 3600));
    } else {
      response.setHeader("Expires", Time.formatForHttp(Time.now()));
    }
  }


  public void destroy() {
    if (_cache != null)
      _cache.flushCache();
  }

}
