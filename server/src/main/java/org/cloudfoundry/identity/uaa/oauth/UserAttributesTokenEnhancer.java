package org.cloudfoundry.identity.uaa.oauth;

import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaUserDatabase;
import org.cloudfoundry.identity.uaa.user.UserInfo;
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
        UserInfo userInfo = userDatabase.getUserInfo(principal.getId());
        if(userInfo!=null && userInfo.getUserAttributes()!=null) {
            return new HashMap(userInfo.getUserAttributes().toSingleValueMap());
        }
        return new HashMap();
    }
}
