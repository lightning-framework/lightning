package lightning.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

public class MimeType {
  private String topLevelTypeName;
  private List<String> tree;
  private String subTypeName;
  private String suffix;
  private Map<String, String> parameters;
  
  public MimeType(String topLevelTypeName, List<String> tree, String subTypeName, 
      String suffix, Map<String, String> parameters) {
    this.topLevelTypeName = topLevelTypeName;
    this.tree = new ArrayList<>(tree);
    this.subTypeName = subTypeName;
    this.suffix = suffix;
    this.parameters = new HashMap<>(parameters);
  }
  
  public String getTopLevelTypeName() {
    return topLevelTypeName;
  }
  
  public List<String> getTree() {
    return Collections.unmodifiableList(tree);
  }
  
  public String getSubTypeName() {
    return subTypeName;
  }
  
  public String getCompleteSubTypeName() {
    if (!tree.isEmpty()) {
      return Joiner.on(".").join(tree) + "." + subTypeName;
    }
    
    return subTypeName;
  }
  
  public String getSuffix() {
    return suffix;
  }
  
  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(parameters);
  }
  
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(topLevelTypeName);
    buffer.append("/");
    
    for (String node : tree) {
      buffer.append(node);
      buffer.append(".");
    }
    
    buffer.append(subTypeName);
    
    if (suffix != null) {
      buffer.append("+");
      buffer.append(suffix);
    }
    
    for (String param : parameters.keySet()) {
      buffer.append("; ");
      buffer.append(param);
      
      String value = parameters.get(param);
      
      if (value != null) {
        buffer.append("=\"");
        buffer.append(value);
        buffer.append("\"");
      }
    }
    
    return buffer.toString();
  }
  
  private enum State {
    START,
    END,
    TOP_LEVEL_NAME,
    TREE_NAME,
    SUFFIX,
    PARAM_NAME,
    PARAM_VALUE,
    PARAM_VALUE_TOKEN,
    PARAM_VALUE_QUOTED,
    PARAM_VALUE_NEXT;
  }
  
  private static String checkComponent(String value) throws MimeParseException {
    if (value.length() == 0) {
      throw new MimeParseException("Empty component.");
    }
    
    return value;
  }
  
  public static MimeType parse(String mimeType) throws MimeParseException {
    int i = 0;
    final char EOF = 0x0;
    State state = State.START;
    StringBuilder topLevelTypeName = new StringBuilder();
    String subTypeName = null;
    String suffix = null;
    StringBuilder suffixBuf = new StringBuilder();
    StringBuilder paramName = new StringBuilder();
    StringBuilder paramValue = new StringBuilder();
    StringBuilder treeName = new StringBuilder();
    List<String> tree = new ArrayList<>();
    Map<String, String> parameters = new HashMap<>();
    
    while (true) {
      char c = (i < mimeType.length() ? mimeType.charAt(i) : EOF);
      
      switch (state) {
        case START:
          state = State.TOP_LEVEL_NAME;
          break;
        case TOP_LEVEL_NAME:
          if (c == '/') {
            state = State.TREE_NAME;
            i++;
          } else if (c == EOF) {
            throw new MimeParseException("Unexpected EOF.");
          } else {
            topLevelTypeName.append(c);
            i++;
          }
          break;
        case TREE_NAME:
          if (c == '.') {
            state = State.TREE_NAME;
            tree.add(checkComponent(treeName.toString().trim()));
            treeName.setLength(0);
            i++;
          } else if (c == '+') {
            state = State.SUFFIX;
            subTypeName = checkComponent(treeName.toString().trim());
            i++;
          } else if (c == ';') {
            state = State.PARAM_NAME;
            subTypeName = checkComponent(treeName.toString().trim());
            i++;
          } else if (c == EOF) {
            state = State.END;
            subTypeName = checkComponent(treeName.toString().trim());
          } else {
            treeName.append(c);
            i++;
          }
          break;
        case SUFFIX:
          if (c == ';') {
            state = State.PARAM_NAME;
            suffix = checkComponent(suffixBuf.toString().trim());
            i++;
          } else if (c == EOF) {
            state = State.END;
            suffix = checkComponent(suffixBuf.toString().trim());
          } else {
            suffixBuf.append(c);
            i++;
          }
          break;
        case PARAM_NAME:
          if (c == '=') {
            state = State.PARAM_VALUE;
            i++;
          } else if (c == ';') {
            state = State.PARAM_NAME;
            parameters.put(checkComponent(paramName.toString().trim()), null);
            paramName.setLength(0);
            i++;
          } else if (c == EOF) {
            parameters.put(checkComponent(paramName.toString().trim()), null);
            state = State.END;
          } else {
            paramName.append(c);
            i++;
          }
          break;
        case PARAM_VALUE:
          if (c == '"') {
            state = State.PARAM_VALUE_QUOTED;
            i++;
          } else {
            state = State.PARAM_VALUE_TOKEN;
          }
          break;
        case PARAM_VALUE_TOKEN:
          if (c == ';') {
            state = State.PARAM_NAME;
            parameters.put(checkComponent(paramName.toString().trim()), 
                checkComponent(paramValue.toString().trim()));
            paramName.setLength(0);
            paramValue.setLength(0);
            i++;
          } else if (c == EOF) {
            state = State.END;
            parameters.put(checkComponent(paramName.toString().trim()), 
                checkComponent(paramValue.toString().trim()));
          } else {
            paramValue.append(c);
            i++;
          }
          break;
        case PARAM_VALUE_QUOTED:
          if (c == '\\') {
            throw new MimeParseException("Escape sequences are not supported.");
          } else if (c == '"') {
            state = State.PARAM_VALUE_NEXT;
            parameters.put(checkComponent(paramName.toString().trim()), 
                checkComponent(paramValue.toString().trim()));
            paramName.setLength(0);
            paramValue.setLength(0);
            i++;
          } else if (c == EOF) {
            throw new MimeParseException("Unexpected EOF.");
          } else {
            paramValue.append(c);
            i++;
          }
          break;
        case PARAM_VALUE_NEXT:
          if (c == ';') {
            state = State.PARAM_NAME;
            i++;
          } else if (c == EOF) {
            state = State.END;
          } else {
            throw new MimeParseException("Unexpected character.");
          }
          break;
        default:
          break;
      }
      
      if (state == State.END) {
        break;
      }
    }
    
    return new MimeType(topLevelTypeName.toString(), tree, subTypeName, suffix, parameters);
  }
  
  public static final class MimeParseException extends Exception {
    private static final long serialVersionUID = 1L;
    public MimeParseException(String msg) {
      super(msg);
    }
  }
}
