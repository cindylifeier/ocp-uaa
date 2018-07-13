package org.cloudfoundry.identity.uaa.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDto {

    @JsonProperty("id")
    String id;

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

    @JsonIgnore
    public UserDto(String id, String givenName, String familyName, String displayName, String description, String info) {
        this.id = id;
        this.givenName = givenName;
        this.familyName = familyName;
        this.displayName = displayName;
        this.description = description;
        this.info = info;
    }

}
