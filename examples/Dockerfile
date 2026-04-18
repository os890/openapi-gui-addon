# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM eclipse-temurin:17-jre

ENV WILDFLY_VERSION=25.0.1.Final \
    JBOSS_HOME=/opt/jboss/wildfly

RUN groupadd -r jboss && useradd -r -g jboss -d /opt/jboss jboss \
    && mkdir -p /opt/jboss \
    && cd /tmp \
    && curl -LO https://github.com/wildfly/wildfly/releases/download/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz -C /opt/jboss \
    && mv /opt/jboss/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:jboss /opt/jboss

COPY stage-runtime-example/target/app1.war $JBOSS_HOME/standalone/deployments/
COPY stage-buildtime-example/target/app2.war $JBOSS_HOME/standalone/deployments/
COPY stage-none-example/target/app3.war $JBOSS_HOME/standalone/deployments/

USER jboss
EXPOSE 8080
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-c", "standalone-microprofile.xml"]
