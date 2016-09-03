package lightning.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lightning.util.MimeType.MimeParseException;

public class MimeMatcher {
  private final class MimeSearchNode {
    private Set<String> matches = new HashSet<>();
    private boolean wildcard = false;
    
    public void add(String spec) {
      if (spec.equals("*")) {
        wildcard = true;
      } else {
        matches.add(spec);
      }
    }
    
    public boolean matches(String s) {
      return wildcard || matches.contains(s.trim());
    }
  }
  
  private final Map<String, MimeSearchNode> table;
  private final MimeSearchNode wildcard;
  
  public MimeMatcher(Iterable<String> types) throws MimeParseException {
    this.table = new HashMap<>();
    this.wildcard = new MimeSearchNode();
    
    for (String type : types) {
      MimeType mime = MimeType.parse(type);
      
      if (mime.getTopLevelTypeName().equals("*")) {
        this.wildcard.add(mime.getCompleteSubTypeName());
      } else {
        if (!this.table.containsKey(mime.getTopLevelTypeName())) {
          this.table.put(mime.getTopLevelTypeName(), new MimeSearchNode());
        }
        
        this.table.get(mime.getTopLevelTypeName()).add(mime.getCompleteSubTypeName());
      }
    }
  }
  
  public boolean matches(String type) {
    MimeType mime;
    
    try {
      mime = MimeType.parse(type);
    } catch (MimeParseException e) {
      return false;
    }
    
    MimeSearchNode node = table.get(mime.getTopLevelTypeName());
    
    if (node == null) {
      return wildcard.matches(mime.getCompleteSubTypeName());
    }
    
    return node.matches(mime.getCompleteSubTypeName());
  }
}
