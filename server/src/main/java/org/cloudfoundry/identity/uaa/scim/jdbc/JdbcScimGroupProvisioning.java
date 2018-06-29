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
package org.cloudfoundry.identity.uaa.scim.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.audit.AuditEventType;
import org.cloudfoundry.identity.uaa.audit.event.AbstractUaaEvent;
import org.cloudfoundry.identity.uaa.audit.event.SystemDeletable;
import org.cloudfoundry.identity.uaa.resources.jdbc.AbstractQueryable;
import org.cloudfoundry.identity.uaa.resources.jdbc.JdbcPagingListFactory;
import org.cloudfoundry.identity.uaa.resources.jdbc.SimpleSearchQueryConverter;
import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimGroupProvisioning;
import org.cloudfoundry.identity.uaa.scim.endpoints.GroupOrScopeDto;
import org.cloudfoundry.identity.uaa.scim.exception.InvalidScimResourceException;
import org.cloudfoundry.identity.uaa.scim.exception.ScimResourceAlreadyExistsException;
import org.cloudfoundry.identity.uaa.scim.exception.ScimResourceConstraintFailedException;
import org.cloudfoundry.identity.uaa.scim.exception.ScimResourceNotFoundException;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.event.IdentityZoneModifiedEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.cloudfoundry.identity.uaa.zone.ZoneManagementScopes.getSystemScopes;
import static org.springframework.util.StringUtils.hasText;

public class JdbcScimGroupProvisioning extends AbstractQueryable<ScimGroup>
    implements ScimGroupProvisioning, SystemDeletable {

    public static final String GROUP = "GROUP";
    public static final String UAA = "uaa";
    private JdbcScimGroupExternalMembershipManager externalGroupMappingManager;
    private JdbcTemplate jdbcTemplate;
    private JdbcScimGroupMembershipManager membershipManager;

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public Log getLogger() {
        return logger;
    }

    public static final String GROUP_FIELDS = "id,displayName,description,created,lastModified,version,identity_zone_id";
    public static final String GROUP_MEMBERSHIP_FIELDS="group_id,member_id,member_type,authorities,added,origin,identity_zone_id";

    public static final String GROUP_TABLE = "groups";
    public static final String GROUP_MEMBERSHIP_TABLE = "group_membership";
    public static final String EXTERNAL_GROUP_TABLE = "external_group_mapping";

    public static final String ADD_GROUP_SQL = String.format(
        "insert into %s ( %s ) values (?,?,?,?,?,?,?)",
        GROUP_TABLE,
        GROUP_FIELDS
    );

    public static final String UPDATE_GROUP_SQL = String.format(
        "update %s set version=?, displayName=?, description=?, lastModified=? where id=? and version=? and identity_zone_id=?",
        GROUP_TABLE
    );

    public static final String GET_GROUP_SQL = String.format(
        "select %s from %s where id=? and identity_zone_id=?",
        GROUP_FIELDS,
        GROUP_TABLE
    );

    public static final String OCP_GET_GROUP_SQL = "select groups.id, groups.displayName, groups.description, group_membership.group_id from groups, group_membership " +
            "where groups.id = group_membership.member_id and groups.displayName ilike '%ocp.role%' and groups.displayName not like '%ocpAdmin%' and groups.displayName not like '%smartUser%' and groups.displayName not like '%smartAdmin%'";

    public static final String OCP_GET_SCOPE_SQL = "select groups.id, groups.displayName, groups.description, group_membership.group_id from groups, group_membership " +
            "where groups.id = group_membership.group_id and groups.displayName ilike '%ocpUiApi%' or groups.displayName like '%smartUser%'";

    public static final String GET_GROUP_BY_NAME_SQL = String.format(
        "select %s from %s where displayName=? and identity_zone_id=?",
        GROUP_FIELDS,
        GROUP_TABLE
    );

    public static final String QUERY_FOR_FILTER = String.format(
        "select %s from %s",
        GROUP_FIELDS,
        GROUP_TABLE
    );

    public static final String DELETE_GROUP_SQL = String.format(
        "delete from %s where id=? and identity_zone_id=?",
        GROUP_TABLE
    );

    public static final String DELETE_GROUP_BY_ZONE = String.format(
        "delete from %s where identity_zone_id=?",
        GROUP_TABLE
    );

    public static final String DELETE_GROUP_MEMBERSHIP_BY_ZONE = String.format(
        "delete from %s where identity_zone_id = ?",
        GROUP_MEMBERSHIP_TABLE
    );

    public static final String DELETE_EXTERNAL_GROUP_BY_ZONE = String.format(
        "delete from %s where identity_zone_id = ?",
        EXTERNAL_GROUP_TABLE
    );

    public static final String DELETE_ZONE_ADMIN_MEMBERSHIP_BY_ZONE = String.format(
        "delete from %s where group_id in (select id from %s where identity_zone_id=? and displayName like ?)",
        GROUP_MEMBERSHIP_TABLE,
        GROUP_TABLE
    );

    public static final String DELETE_ZONE_ADMIN_GROUPS_BY_ZONE = String.format(
        "delete from %s where identity_zone_id=? and displayName like ?",
        GROUP_TABLE
    );

    public static final String DELETE_GROUP_MEMBERSHIP_BY_PROVIDER = String.format(
        "delete from %s where identity_zone_id = ? and origin = ?",
        GROUP_MEMBERSHIP_TABLE
    );


    public static final String DELETE_EXTERNAL_GROUP_BY_PROVIDER = String.format(
        "delete from %s where identity_zone_id = ? and origin = ?",
        EXTERNAL_GROUP_TABLE,
        GROUP_TABLE
    );

    public static final String DELETE_MEMBER_SQL = String.format(
        "delete from %s where member_id=? and member_id in (select id from users where id=? and identity_zone_id=?)",
        GROUP_MEMBERSHIP_TABLE
    );

    public static final String ADD_OCP_SCOPE_SQL = String.format(
        "insert into %s ( %s ) values (?, ?, ?, ?, ?, ?, ?)",
        GROUP_MEMBERSHIP_TABLE,
        GROUP_MEMBERSHIP_FIELDS
    );

    private final RowMapper<ScimGroup> rowMapper = new ScimGroupRowMapper();

    public JdbcScimGroupProvisioning(JdbcTemplate jdbcTemplate, JdbcPagingListFactory pagingListFactory) {
        super(jdbcTemplate, pagingListFactory, new ScimGroupRowMapper());

        this.membershipManager = new JdbcScimGroupMembershipManager(jdbcTemplate);
        this.membershipManager.setScimGroupProvisioning(this);
        this.externalGroupMappingManager = new JdbcScimGroupExternalMembershipManager(jdbcTemplate);
        this.externalGroupMappingManager.setScimGroupProvisioning(this);

        Assert.notNull(jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        setQueryConverter(new SimpleSearchQueryConverter());
    }

    public void createAndIgnoreDuplicate(final String name, final String zoneId) {
        try {
            create(new ScimGroup(null, name, zoneId), zoneId);
        }catch (ScimResourceAlreadyExistsException ignore){
        }
    }

    @Override
    public ScimGroup createOrGet(ScimGroup group, String zoneId) {
        try {
            return getByName(group.getDisplayName(), zoneId);
        } catch (IncorrectResultSizeDataAccessException e) {
            createAndIgnoreDuplicate(group.getDisplayName(), zoneId);
            return getByName(group.getDisplayName(), zoneId);
        }
    }

    @Override
    public ScimGroup getByName(String displayName, String zoneId) {
        if (!hasText(displayName)) {
            throw new IncorrectResultSizeDataAccessException("group name must contain text", 1, 0);
        }
        List<ScimGroup> groups = jdbcTemplate.query(GET_GROUP_BY_NAME_SQL, rowMapper, displayName, zoneId);
        if (groups.size()==1) {
            return groups.get(0);
        } else {
            throw new IncorrectResultSizeDataAccessException("Invalid result size found for:"+displayName, 1, groups.size());
        }
    }

    @Override
    public void onApplicationEvent(AbstractUaaEvent event) {
        if (event!=null && event instanceof IdentityZoneModifiedEvent) {
            IdentityZoneModifiedEvent zevent = (IdentityZoneModifiedEvent)event;
            if (zevent.getEventType() == AuditEventType.IdentityZoneCreatedEvent) {
                final String zoneId = ((IdentityZone) event.getSource()).getId();
                getSystemScopes().stream().forEach(
                    scope -> createAndIgnoreDuplicate(scope, zoneId)
                );
            }
        }
        SystemDeletable.super.onApplicationEvent(event);
    }

    @Override
    protected String getBaseSqlQuery() {
        return QUERY_FOR_FILTER;
    }

    @Override
    protected String getTableName() {
        return GROUP_TABLE;
    }


    @Override
    public List<ScimGroup> retrieveAll(final String zoneId) {
        return query("id pr", "created", true, zoneId);
    }

    @Override
    public ScimGroup retrieve(String id, final String zoneId) throws ScimResourceNotFoundException {
        try {
            ScimGroup group = jdbcTemplate.queryForObject(GET_GROUP_SQL, rowMapper, id, zoneId);
            return group;
        } catch (EmptyResultDataAccessException e) {
            throw new ScimResourceNotFoundException("Group " + id + " does not exist");
        }
    }

    @Override
    public ScimGroup create(final ScimGroup group, final String zoneId) throws InvalidScimResourceException {
        final String id = UUID.randomUUID().toString();
        logger.debug("creating new group with id: " + id);
        try {
            validateGroup(group);
            jdbcTemplate.update(ADD_GROUP_SQL, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    int pos = 1;
                    ps.setString(pos++, id);
                    ps.setString(pos++, group.getDisplayName());
                    ps.setString(pos++, group.getDescription());
                    ps.setTimestamp(pos++, new Timestamp(new Date().getTime()));
                    ps.setTimestamp(pos++, new Timestamp(new Date().getTime()));
                    ps.setInt(pos++, group.getVersion());
                    ps.setString(pos++, zoneId);
                }
            });
        } catch (DuplicateKeyException ex) {
            throw new ScimResourceAlreadyExistsException("A group with displayName: " + group.getDisplayName()
                            + " already exists.");
        }
        return retrieve(id, zoneId);
    }

    @Override
    public ScimGroup update(final String id, final ScimGroup group, final String zoneId) throws InvalidScimResourceException,
                    ScimResourceNotFoundException {
        try {
            validateGroup(group);

            int updated = jdbcTemplate.update(UPDATE_GROUP_SQL, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    int pos = 1;
                    ps.setInt(pos++, group.getVersion() + 1);
                    ps.setString(pos++, group.getDisplayName());
                    ps.setString(pos++, group.getDescription());
                    ps.setTimestamp(pos++, new Timestamp(new Date().getTime()));
                    ps.setString(pos++, id);
                    ps.setInt(pos++, group.getVersion());
                    ps.setString(pos++, zoneId);
                }
            });
            if (updated != 1) {
                throw new IncorrectResultSizeDataAccessException(1, updated);
            }
            return retrieve(id, zoneId);
        } catch (DuplicateKeyException ex) {
            throw new InvalidScimResourceException("A group with displayName: " + group.getDisplayName()
                            + " already exists");
        }
    }

    @Override
    public ScimGroup delete(String id, int version, String zoneId) throws ScimResourceNotFoundException {
        ScimGroup group = retrieve(id, zoneId);
        membershipManager.removeMembersByGroupId(id, zoneId);
        externalGroupMappingManager.unmapAll(id, zoneId);
        int deleted;
        if (version > 0) {
            deleted = jdbcTemplate.update(DELETE_GROUP_SQL + " and version=?;", id, zoneId,version);
        } else {
            deleted = jdbcTemplate.update(DELETE_GROUP_SQL, id, zoneId);
        }
        if (deleted != 1) {
            throw new IncorrectResultSizeDataAccessException(1, deleted);
        }
        return group;
    }

    public int deleteByIdentityZone(String zoneId) {
        jdbcTemplate.update(DELETE_ZONE_ADMIN_MEMBERSHIP_BY_ZONE, IdentityZone.getUaa().getId(), "zones." + zoneId + ".%");
        jdbcTemplate.update(DELETE_ZONE_ADMIN_GROUPS_BY_ZONE, IdentityZone.getUaa().getId(), "zones." + zoneId + ".%");
        jdbcTemplate.update(DELETE_EXTERNAL_GROUP_BY_ZONE, zoneId);
        jdbcTemplate.update(DELETE_GROUP_MEMBERSHIP_BY_ZONE, zoneId);
        return jdbcTemplate.update(DELETE_GROUP_BY_ZONE, zoneId);
    }

    public int deleteByOrigin(String origin, String zoneId) {
        jdbcTemplate.update(DELETE_EXTERNAL_GROUP_BY_PROVIDER, zoneId, origin);
        return jdbcTemplate.update(DELETE_GROUP_MEMBERSHIP_BY_PROVIDER, zoneId, origin);
    }

    @Override
    public int deleteByUser(String userId, String zoneId) {
        int result = jdbcTemplate.update(DELETE_MEMBER_SQL, userId, userId, zoneId);

        return result;
    }

    protected void validateGroup(ScimGroup group) throws ScimResourceConstraintFailedException {
        if (!hasText(group.getZoneId())) {
            throw new ScimResourceConstraintFailedException("zoneId is a required field");
        }
    }

    @Override
    protected void validateOrderBy(String orderBy) throws IllegalArgumentException {
        super.validateOrderBy(orderBy, GROUP_FIELDS);
    }

    @Override
    public void createScopesOrRoles(List<String> scopes, String groupId, String memberType) throws SQLException {
        scopes.stream().forEach(scope -> {
            jdbcTemplate.update(ADD_OCP_SCOPE_SQL, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    int pos = 1;
                    ps.setString(pos++, scope); //group_id = scopes or roles
                    ps.setString(pos++, groupId); //member_id = groupId or userId
                    ps.setString(pos++, memberType); //member_type = GROUP or USER
                    ps.setString(pos++, null);
                    ps.setTimestamp(pos++, new Timestamp(new Date().getTime()));
                    ps.setString(pos++, UAA);
                    ps.setString(pos++, UAA);
                }
            });
        });
    }

    @Override
    public List<GroupOrScopeDto> getOcpGroups() {
        return getGroupsOrScopes(OCP_GET_GROUP_SQL);
    }

    public List<GroupOrScopeDto> getOcpScopes() {
        return getGroupsOrScopes(OCP_GET_SCOPE_SQL);
    }

    private List<GroupOrScopeDto> getGroupsOrScopes(final String sql) {
        return jdbcTemplate.query(sql, new RowMapper<GroupOrScopeDto>() {
            @Override
            public GroupOrScopeDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                int pos = 1;
                String id = rs.getString(pos++);
                String displayName = rs.getString(pos++);
                String description = rs.getString(pos++);
                String scopeId = rs.getString(pos++);
                return new GroupOrScopeDto(id, displayName, description, scopeId);
            }
        });
    }

}
