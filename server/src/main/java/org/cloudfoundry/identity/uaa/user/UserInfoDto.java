package org.cloudfoundry.identity.uaa.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfoDto {

    @JsonProperty("userId")
    String userId;

    @JsonProperty("info")
    String info;

    @JsonIgnore
    public String getUserId() {
        return userId;
    }

    @JsonIgnore
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonIgnore
    public String getInfo() {
        return info;
    }

    @JsonIgnore
    public void setInfo(String info) {
        this.info = info;
    }
}
