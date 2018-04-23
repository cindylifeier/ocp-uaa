# A note on using the scripts present in this directory

There are two scripts in this directory. One, `create_role_with_scopes.sh`, is used to create role for a selected organization ( or a `ocpAdmin` who does not need to be associated with an organization). Another, `create-user-with-attributes.sh` is used to associate a Practitioner or a Patient with the created role.

Note: At the moment, once a role is created for an organization, we do not support editing or deleting the role. However, you may change the `create_role_with_scopes.sh` script to add more scopes for a role before creating a new one. When you attempt to create a new role that's already present, the script simply exits.

## Steps
1. Run the script using Git Bash: `./create_role_with_scopes.sh`. Creating each role takes a few minutes.  Be patient while the script finishes. You may create all of the desired roles one-by-one at this stage. If you are asked to enter an Organization ID, enter the FHIR resource ID of the Organization to which you want to associate the role. Be sure to use this Organization ID in Step 5 and 6 in place of `<orgId>`. 
2.	Next, run `./create-user-with-attributes.sh`. Begin by creating OCP Admin. Enter First Name, Last Name, Email, User Name, Password (be sure to remember the user name and password). For the role scope, enter `ocp.role.ocpAdmin` and then select resource type as 3 (NONE).
3.	Login to OCP UI as OCP Admin. Go ahead and create Practitioners with Care Coordinator, Care Manager and Org Admin role. Hint: Write down the FHIR resource ID for each of the practitioners you create. The easiest way to find the IDs is to click on the “Edit” menu for the newly-created practitioners and checking the URL.
4.	Also, create a patient from OCP UI and write down the FHIR resource ID.
5.	You may now create a login account for Care Coordinator, Care Manager, and Org Admin role by  running ./create-user-with-attributes.sh. Scopes to be used are `ocp.role.organization.<orgId>.careCoordinator`, `ocp.role.organization.<orgId>.careManager`, `ocp.role.organization.<orgId>.organizationAdministrator`, respectively. When prompted, select the resource type as 1 (Practitioner) and enter the designated FHIR ID.
6.	Similar to the step above, create a login account for the Patient role by running `./create-user-with-attributes.sh`. Scope to be used is `ocp.role.organization.<orgId>.patient`. When prompted, select resource type as 2 (Patient) and enter the designated FHIR ID.
