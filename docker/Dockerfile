FROM tomcat:8-jre8
# Default CLOUD_FOUNDRY_CONFIG_PATH which uaa.yml located.
ENV CLOUD_FOUNDRY_CONFIG_PATH /java/OCP_PROPS/uaa
# Copy uaa.war to container
ADD uaa.war /usr/local/tomcat/webapps/