package org.cloudfoundry.identity.uaa.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDto {

    @JsonProperty("id")
    String id;

    @JsonProperty("username")
    String username;

    @JsonProperty("givenName")
    String givenName;

    @JsonProperty("familyName")
    String familyName;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("description")
    String description;

    @JsonProperty("info")
    String info;

    @JsonProperty("groupId")
    String groupId;

    @JsonIgnore
    public UserDto(String id, String username, String givenName, String familyName, String displayName, String description, String info, String groupId) {
        this.id = id;
        this.username = username;
        this.givenName = givenName;
        this.familyName = familyName;
        this.displayName = displayName;
        this.description = description;
        this.info = info;
        this.groupId = groupId;
    }

}
