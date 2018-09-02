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
package org.cloudfoundry.identity.uaa.user;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for loading user data for the UAA.
 */
public interface UaaUserDatabase {
    UaaUser retrieveUserByName(String username, String origin) throws UsernameNotFoundException;

    UaaUser retrieveUserById(String id) throws UsernameNotFoundException;

    UaaUser retrieveUserByEmail(String email, String origin) throws UsernameNotFoundException;

    UserInfo getUserInfo(String id);

    List<UserDto> getUsersByOrganizationId(String organizationId, String resource);

    List<UserDto> getUsersByFhirResource(String resourceId, String resource);

    UserInfo storeUserInfo(String id, UserInfo info);

    void updateLastLogonTime(String id);

    Object retrievePractitionersByOrganizationAndRole(String organizationId, String uaaRole);

    Map<String, List<UserDto>> getUserRoles(List<String> fhirIds);
}
