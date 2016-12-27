package lightning.io;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lightning.enums.CacheControl;

import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpContent.ContentFactory;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.CachedContentFactory;
import org.eclipse.jetty.server.ResourceContentFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.ResourceService.WelcomeFactory;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

import com.google.common.collect.ImmutableList;

public class FileServer implements ResourceFactory, WelcomeFactory {
  private static final Logger LOG = Log.getLogger(FileServer.class);
  private boolean isCachingEnabled;
  
  private final ResourceFactory _resourceFactory;
  private final ResourceService _resourceService;
  private final CachedContentFactory _cache;
  private final MimeTypes _mimeTypes;
  private final CompressedContentFormat[] _precompressedFormats;
  private final List<String> _gzipEquivalentFileExtensions;

  public FileServer(ResourceFactory resourceFactory) {
    _mimeTypes = new MimeTypes();
    _precompressedFormats = new CompressedContentFormat[]{ CompressedContentFormat.GZIP,
                                                           CompressedContentFormat.BR };
    _gzipEquivalentFileExtensions = ImmutableList.of(".svgz");
    _resourceFactory = resourceFactory;
    _cache = new CachedContentFactory(null, this, _mimeTypes, true, true, _precompressedFormats);
    _resourceService = createService(CacheControl.PUBLIC, _cache);
  }
  
  private ResourceService createService(CacheControl cacheControl, 
                                        ContentFactory contentFactory) {
    ResourceService resourceService = new ResourceService();
    resourceService.setAcceptRanges(true);
    resourceService.setCacheControl(cacheControl.toHttpField());
    resourceService.setDirAllowed(false);
    resourceService.setWelcomeFactory(this);
    resourceService.setRedirectWelcome(false);
    resourceService.setPrecompressedFormats(_precompressedFormats);
    resourceService.setPathInfoOnly(true);
    resourceService.setGzipEquivalentFileExtensions(_gzipEquivalentFileExtensions);
    resourceService.setEncodingCacheSize(100);
    resourceService.setEtags(true);
    resourceService.setContentFactory(contentFactory);
    return resourceService;
  }

  public void sendResource(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Resource resource,
                           CacheControl cacheControl) throws ServletException, IOException {
    // TODO: Would be nice to re-use the existing _cache.
    
    ResourceFactory factory = new ResourceFactory() {
      @Override
      public Resource getResource(String path) {
        return resource;
      }
    };
    ResourceContentFactory contentFactory = new ResourceContentFactory(factory, _mimeTypes, new CompressedContentFormat[]{});
    ResourceService service = createService(cacheControl, contentFactory);
    service.doGet(request, response);
  }

  public boolean couldConsume(HttpServletRequest request, HttpServletResponse response) {
    String path = getPathInContext(request);
    
    if (isCachingEnabled) {
      try {
        HttpContent content = _cache.getContent(path, response.getBufferSize());
        return content != null;
      } catch (IOException e) {
        LOG.warn(e);
        return false;
      }    
    }
    
    Resource resource = getResource(path);
    return resource != null && resource.exists() && !resource.isDirectory();
  }
  
  @Override
  public Resource getResource(String pathInContext) {
    return _resourceFactory.getResource(pathInContext);
  }
  
  private String getPathInContext(HttpServletRequest request) {
    return request.getPathInfo();
  }
  

  public void setMaxCacheSize(int maxCacheSize) {
    _cache.setMaxCacheSize(maxCacheSize);
  }
  

  public void setMaxCachedFiles(int maxCachedFiles) {
    _cache.setMaxCachedFiles(maxCachedFiles);
  }

  public void setMaxCachedFileSize(int maxCachedFileSize) {
    _cache.setMaxCachedFileSize(maxCachedFileSize);
  }

  public void usePublicCaching() {
    isCachingEnabled = true;
    _cache.flushCache();
    _resourceService.setCacheControl(CacheControl.PUBLIC.toHttpField());
  }

  public void disableCaching() {
    isCachingEnabled = false;
    _cache.flushCache();
    _resourceService.setCacheControl(CacheControl.NO_CACHE.toHttpField());
  }
  
  public void destroy() {
    _cache.flushCache();
  }

  @Override
  public String getWelcomeFile(String pathInContext) {
    return null; // Disallow directory indexes.
  }

  public void handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    _resourceService.doGet(request, response);
  }
}
