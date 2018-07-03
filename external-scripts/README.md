# A note on using the scripts present in this directory

There are five scripts in this directory. 
1. `create-role-with-scopes.sh`, is used to create role for a selected organization ( or a `ocpAdmin` who does not need to be associated with an organization).
2. `create-user-with-attributes.sh` is used to associate a Practitioner or a Patient with the created role allowing you to login to the OCP UI.
3. `add-role-as-member-of-scope.sh` is used to add additional scopes to a existing role for OCP UI.
4. `create-smart-admin-role-with-scope.sh` is used to setup smart admin role to register Smart On FHIR apps on OCP Smart On FHIR platform.
5. `create-smart-role-with-scope.sh` is used to setup smart user role to launch Smart On FHIR apps.

Note: At the moment, once a role is created for an organization, we do not support editing or deleting the role. However, you may change the `create-role-with-scopes.sh` script to add more scopes for a role before creating a new one. Further, you can also add additional scopes to an existing role using the script `add-role-as-member-of-scope.sh`. When you attempt to create a new role that's already present, the script simply exits.

## Steps to setup OCP role and scopes
1.  Run the script using Git Bash: `./create-role-with-scopes.sh`. Creating each role takes a few minutes.  Be patient while the script finishes. You may create all of the desired roles one-by-one at this stage. 
2.	Next, run `./create-user-with-attributes.sh`. Begin by creating OCP Admin. Enter First Name, Last Name, Email, User Name, Password (be sure to remember the user name and password). For the role scope, enter `ocp.role.ocpAdmin` and then select resource type as 3 (NONE).
3.	Login to OCP UI as OCP Admin. Go ahead and create Practitioners with any of the following roles: Care Coordinator, Care Manager, Org Administrator, Primary Care Provider, Benefits Specialist, Health Assistant and Front Office Receptionist. *Hint:* Write down the FHIR resource ID for each of the practitioners you create. The easiest way to find the IDs is to click on the “Edit” menu for the newly-created Practitioners and checking the URL.
4.	You may now create a login account for (any or all) Care Coordinator, Care Manager, Org Admin, Primary Care Provider, Benefits Specialist, Health Assistant and Front Office Receptionist role by  running `./create-user-with-attributes.sh`. Scopes to be used are `ocp.role.careCoordinator`, `ocp.role.careManager`, `ocp.role.organizationAdministrator`, `ocp.role.primaryCareProvider`, `ocp.role.benefitsSpecialist`, `ocp.role.healthAssistant` and `ocp.role.frontOfficeReceptionist`, respectively. When prompted, select the resource type as 1 (Practitioner), the Organization ID to which you want to associate the role and then, the designated FHIR ID of the Practitioner.
5.	If desired, you may now create a patient from OCP UI and write down the FHIR resource ID.
6.	Similar to the Step #4, create a login account for the Patient by running `./create-user-with-attributes.sh`. Scope to be used is `ocp.role.patient`. When prompted, select resource type as 2 (Patient), the Organization ID to which you want to associate the role and then, the designated FHIR ID of the Patient.
7.  At the end of Steps #4 and #6, you will be able to login to OCP UI using the login credentials that have specific role associated with them.

## Steps to setup Smart On FHIR user and admin
OCP is a Smart On FHIR platform. To register Smart On FHIR apps and launch Smart On FHIR apps, please follow the steps to setup smart user and smart amin scopes.
1. Run the script using Git Bash: `create-smart-admin-role-with-scope.sh`. This will create smart admin role (`ocp.role.smartAdmin`) associates with register client scopes, then assign to OCP admin with role `ocp.role.ocpAmin` and allow OCP admin users to register Smart On FHIR apps.
2. Run the script using Git Bash: `create-smart-role-with-scope.sh`. This will create smart user role (`ocp.role.smartUser`) associates with Smart On FHIR scopes and assign to ocp users to launch Smart On FHIR apps.

   
