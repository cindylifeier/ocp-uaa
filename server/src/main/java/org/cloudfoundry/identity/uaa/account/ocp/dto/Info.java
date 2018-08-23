package org.cloudfoundry.identity.uaa.account.ocp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Info {

    @JsonProperty("roles")
    private Object roles;

    @JsonProperty("user_attributes")
    private UserAttributes userAttributes;

    public Object getRoles() {
        return roles;
    }

    public void setRoles(Object roles) {
        this.roles = roles;
    }

    public UserAttributes getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(UserAttributes userAttributes) {
        this.userAttributes = userAttributes;
    }

}
