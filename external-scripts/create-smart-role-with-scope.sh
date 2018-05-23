#!/bin/bash

UAA_BASE_URL="http://localhost:8080/uaa"
CLIENT_ID="admin-client"
CLIENT_SECTRET="changeit"
ADMIN_USERNAME="scim-admin"
ADMIN_PASSWORD="AAA!aaa1"
RESPONSE_TYPE="token"
GRANT_TYPE="password"

SMART_ROLE_NAME="ocp.role.smartUser"
SMART_ROLE_DESCRIPTION="ocp smart user role"

ocpSmartScope=(launch launch/patient launch/location launch/organization patient/Patient.read patient/Patient.write patient/RelatedPerson.read patient/RelatedPerson.write patient/EpisodeOfCare.read patient/EpisodeOfCare.write patient/Coverage.read patient/Coverage.write patient/Communication.read patient/Communication.write patient/Flag.read patient/Flag.write patient/ActivityDefinition.read patient/ActivityDefinition.write patient/Task.read patient/Task.write patient/Appointment.read patient/Appointment.write patient/AllergyIntolerence.read patient/AllergyIntolerence.write patient/Condition.read patient/Condition.write patient/Procedure.read patient/Procedure.write patient/ClinicalImpression.read patient/ClinicalImpression.write patient/Observation.read patient/Observation.write patient/Specimen.read patient/Specimen.write patient/DiagnosticReport.read patient/DiagnosticReport.write patient/Medication.read patient/Medication.write patient/MedicationRequest.read patient/MedicationRequest.write patient/MedicationDispense.read patient/MedicationDispense.write patient/MedicationStatement.read patient/MedicationStatement.write patient/CareTeam.read patient/CareTeam.write patient/CarePlan.read patient/CarePlan.write patient/Goal.read patient/Goal.write patient/ReferralRequest.read patient/ReferralRequest.write patient/ProcedureRequest.read patient/ProcedureRequest.write patient/AuditEvent.read patient/AuditEvent.write patient/Consent.read patient/Consent.write  patient/Provenance.read patient/Provenance.write)

function showInformation()
{
echo -e "This script will setup ocp.role.smartUser and assign ocp.role.smartUser to one existing role"
}

function jsonValue() {
KEY=$1
awk -F"[{,:}]" '{for(i=1;i<=NF;i++){if($i~/'$KEY'/){print $(i+1)}}}'|tr -d '"'| sed -n 1p
}

#Get user Token
function getScimAdminToken(){
token=$(curl --silent\
        -H "Content-Type:application/x-www-form-urlencoded" \
		-H "Cache-Control: no-cache" \
        -X POST --data "client_id=$CLIENT_ID&grant_type=$GRANT_TYPE&client_secret=$CLIENT_SECTRET&username=$ADMIN_USERNAME&response_type=$RESPONSE_TYPE&password=$ADMIN_PASSWORD" "$UAA_BASE_URL/oauth/token" | jsonValue access_token)
echo -e $token

}

function getGroupId(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
result=$(curl --silent \
	-H "Authorization: $2" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")
groupId=`echo  -e $result | grep -Po '"id": *\K"[^"]*"'  | tr -d '"'`
echo -e $groupId
}

function createGroupMembership(){
response=$(curl --silent \
			-H "Authorization: $3" \
			-H "Cache-Control: no-cache" \
			-H "Content-Type: application/json" \
			POST --data '{"origin" : "uaa", "type" : "GROUP", "value" : "'"$2"'"}' "$UAA_BASE_URL/Groups/$1/members")
}

function createNewRole(){
roleId=$(curl --silent \
	-H "Cache-Control: no-cache" \
	-H "Content-Type: application/json" \
	-H "Authorization: $3" \
	POST --data '{"displayName": "$1", "description" : "$2"}' "$UAA_BASE_URL/Groups" | jsonValue id)
}

function createRoleAndAddScopes(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
#echo -e $replacedString
totalResults=$(curl --silent \
	-H "Authorization: $3" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22" | jsonValue totalResults)

echo -e $(curl --silent \
	-H "Authorization: $3" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")

if [ "$totalResults" -eq 0 ]; then
	roleId=$(curl --silent \
		-H "Cache-Control: no-cache" \
		-H "Content-Type: application/json" \
		-H "Authorization: $3" \
		POST --data '{"displayName": "'"$1"'", "description" : "'"$2"'"}' "$UAA_BASE_URL/Groups"  | jsonValue id)

echo -e "Created the new role."

echo -e "Adding permissions to $1"
echo "Number of permissions to be added: ${#ocpSmartScope[*]}"
for index in ${!ocpSmartScope[*]}
	do
		groupId=`getGroupId "${ocpSmartScope[$index]}" "${bearerToken}"`
#		echo -e $groupId
		createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
		echo -e "\n Done: $((index+1)) of ${#ocpSmartScope[*]}"
	done

else
   echo "Role already present. Exitting!"
fi
}

function assignSmartRoleToExistingRole(){
  smartUserId=`getGroupId "${SMART_ROLE_NAME}" "${bearerToken}"`
#  echo -e $smartUserId
  existingRoleId=`getGroupId "$1" "${bearerToken}"`
#  echo -e $existingRoleId
  createGroupMembership "${smartUserId}" "${existingRoleId}" "${bearerToken}"
}

clear
token=`getScimAdminToken`
bearerToken="bearer $token"
#echo $bearerToken
showInformation

echo -n "Please enter existing role which will launch Smart on Fhir app (eg: ocp.role.ocpAdmin) : "
read existingRole

echo -e "Step 1 of 2: Create role and assign smart scope"
echo -e "The smart role created will be: ${SMART_ROLE_NAME}"
createRoleAndAddScopes "${SMART_ROLE_NAME}" "${SMART_ROLE_DESCRIPTION}" "${bearerToken}"

echo -e "Step 2 of 2: Assign smartUser to existing ocp role"
assignSmartRoleToExistingRole "${existingRole}"

echo -e "DONE"

