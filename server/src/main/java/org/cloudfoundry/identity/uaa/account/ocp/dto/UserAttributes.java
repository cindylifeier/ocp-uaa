package org.cloudfoundry.identity.uaa.account.ocp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAttributes {

    @JsonProperty("resource")
    private List<String> resource = null;

    @JsonProperty("id")
    private List<String> id = null;

    @JsonProperty("orgId")
    private List<String> orgId = null;

    public List<String> getResource() {
        return resource;
    }

    public void setResource(List<String> resource) {
        this.resource = resource;
    }

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    public List<String> getOrgId() {
        return orgId;
    }

    public void setOrgId(List<String> orgId) {
        this.orgId = orgId;
    }

}
