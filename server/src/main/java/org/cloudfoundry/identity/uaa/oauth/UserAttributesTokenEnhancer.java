package org.cloudfoundry.identity.uaa.oauth;

import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaUserDatabase;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.HashMap;
import java.util.Map;

public class UserAttributesTokenEnhancer implements UaaTokenEnhancer{

    private UaaUserDatabase userDatabase;

    public void setUserDatabase(UaaUserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    @Override
    public Map<String, String> getExternalAttributes(OAuth2Authentication authentication) {
        UaaPrincipal principal= (UaaPrincipal) authentication.getPrincipal();
        return new HashMap(userDatabase.getUserInfo(principal.getId()).getUserAttributes().toSingleValueMap());
    }
}
