package org.cloudfoundry.identity.uaa.oauth;

import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaUserDatabase;
import org.cloudfoundry.identity.uaa.user.UserInfo;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.HashMap;
import java.util.Map;

public class UserAttributesTokenEnhancer implements UaaTokenEnhancer {

    private UaaUserDatabase userDatabase;

    public void setUserDatabase(UaaUserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    @Override
    public Map<String, String> getExternalAttributes(OAuth2Authentication authentication) {
        final Object principal = authentication.getPrincipal();
        if (principal != null && principal instanceof UaaPrincipal) {
            final UaaPrincipal uaaPrincipal = (UaaPrincipal) authentication.getPrincipal();
            UserInfo userInfo = userDatabase.getUserInfo(uaaPrincipal.getId());
            if (userInfo != null && userInfo.getUserAttributes() != null) {
                return new HashMap(userInfo.getUserAttributes().toSingleValueMap());
            }
            return new HashMap();
        }
        return null;
    }
}
