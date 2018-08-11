/*******************************************************************************
 *     Cloud Foundry
 *     Copyright (c) [2009-2016] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.account;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaUser;
import org.cloudfoundry.identity.uaa.user.UaaUserDatabase;
import org.cloudfoundry.identity.uaa.user.UserInfo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.expression.OAuth2ExpressionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.security.Principal;
import java.util.Optional;

import static org.cloudfoundry.identity.uaa.oauth.token.ClaimConstants.ROLES;
import static org.cloudfoundry.identity.uaa.oauth.token.ClaimConstants.USER_ATTRIBUTES;


/**
 * Controller that sends user info to clients wishing to authenticate.
 *
 * @author Dave Syer
 */
@Controller
public class UserInfoEndpoint implements InitializingBean {

    public static final String UAA_ADMIN = "uaa.admin";
    public static final String SCIM_WRITE = "scim.write";
    public static final String USER_ID_KEY = "user_id";
    private static Log logger = LogFactory.getLog(UserInfoEndpoint.class);

    private UaaUserDatabase userDatabase;

    public void setUserDatabase(UaaUserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(userDatabase != null, "A user database must be provided");
    }

    @RequestMapping(value = "/userinfo" )
    @ResponseBody
    public UserInfoResponse loginInfo(Principal principal) {
        OAuth2Authentication authentication = (OAuth2Authentication) principal;
        UaaPrincipal uaaPrincipal = extractUaaPrincipal(authentication);
        boolean addCustomAttributes = OAuth2ExpressionUtils.hasAnyScope(authentication, new String[] {USER_ATTRIBUTES});
        boolean addRoles = OAuth2ExpressionUtils.hasAnyScope(authentication, new String[] {ROLES});
        return getResponse(uaaPrincipal, addCustomAttributes, addRoles);
    }

    @RequestMapping(value = "/userinfos")
    @ResponseBody
    public Object getUsersByOrganizationId(@RequestParam(required = true, value = "organizationId") String organizationId, @RequestParam(value = "resource", required = true) String resource) {
        return userDatabase.getUsersByOrganizationId(organizationId, resource);
    }

    protected UaaPrincipal extractUaaPrincipal(OAuth2Authentication authentication) {
        Object object = authentication.getUserAuthentication().getPrincipal();
        if (object instanceof UaaPrincipal) {
            return (UaaPrincipal) object;
        }
        throw new IllegalStateException("User authentication could not be converted to UaaPrincipal");
    }

    protected UserInfoResponse getResponse(UaaPrincipal principal, boolean addCustomAttributes, boolean addRoles) {
        UaaUser user = userDatabase.retrieveUserById(principal.getId());
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setGivenName(user.getGivenName());
        response.setFamilyName(user.getFamilyName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setSub(user.getId());
        response.setPreviousLogonSuccess(user.getPreviousLogonTime());

        UserInfo info = userDatabase.getUserInfo(user.getId());
        if (addCustomAttributes && info!=null) {
            response.setAttributeValue(USER_ATTRIBUTES, info.getUserAttributes());
        }
        if (addRoles && info!=null) {
            response.setAttributeValue(ROLES, info.getRoles());
        }
        return response;
    }

    @RequestMapping(value = "/userinfo", method = RequestMethod.POST )
    @ResponseStatus(HttpStatus.OK)
    public void loginInfo(@RequestBody LinkedMultiValueMap<String, String> userAttributes, Principal principal) {
        OAuth2Authentication authentication = (OAuth2Authentication) principal;
        if(authentication.getUserAuthentication()!=null) {
            UaaPrincipal uaaPrincipal = extractUaaPrincipal(authentication);
        }
        boolean addCustomAttributes = OAuth2ExpressionUtils.hasAnyScope(authentication, new String[]{UAA_ADMIN,SCIM_WRITE});
        if(addCustomAttributes==false)
            throw new IllegalStateException("Permission not enough to create userinfo");
        String userId=userAttributes.getFirst(USER_ID_KEY);
        userAttributes.remove(USER_ID_KEY);
        userDatabase.storeUserInfo(userId, new UserInfo().setUserAttributes(userAttributes));

    }

    @RequestMapping(value = "/practitionerByOrganizationAndRole")
    @ResponseBody
    public Object retrievePractitionersByOrganizationAndRole(@RequestParam(required = true, value = "organization") String organizationId, @RequestParam(required = false, value = "role") String uaaRole) {
        return userDatabase.retrievePractitionersByOrganizationAndRole(organizationId, uaaRole);
    }
}

