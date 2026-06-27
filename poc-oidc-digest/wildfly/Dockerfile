# WildFly 39 (Jakarta EE 11) — matches the addon and the repo's examples.
FROM quay.io/wildfly/wildfly:39.0.0.Final-jdk21

USER root

# Digest realm credential files (read by the Elytron properties-realm).
COPY poc-users.properties poc-groups.properties /opt/jboss/wildfly/standalone/configuration/

# Apply the Elytron DIGEST + elytron-oidc-client configuration to standalone-microprofile.xml.
COPY configure.cli /opt/jboss/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/configure.cli

# Deploy the two module WARs (built by build_and_run.sh into ./deployments).
# Each WAR ships the addon and serves its own shared Swagger UI.
COPY deployments/module-a.war /opt/jboss/wildfly/standalone/deployments/
COPY deployments/module-b.war /opt/jboss/wildfly/standalone/deployments/

RUN chown -R jboss:jboss /opt/jboss/wildfly/standalone

USER jboss
EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-c", "standalone-microprofile.xml"]
