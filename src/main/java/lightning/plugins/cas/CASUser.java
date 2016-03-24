package lightning.plugins.cas;

import com.google.common.collect.ImmutableMap;

public final class CASUser {
  public final String username;
  public final ImmutableMap<String, String> properties;
  public final String destinationUrl;
  
  CASUser(String username, ImmutableMap<String, String> properties, String destinationUrl) {
    this.username = username;
    this.properties = properties;
    this.destinationUrl = destinationUrl;
  }
}
