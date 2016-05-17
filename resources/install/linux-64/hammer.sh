#!/bin/sh

# TODO add check for required stuff

# Create keystore files
if [ ! -f $KEYSTORE_FILE ]
then
    echo "Creating $KEYSTORE_FILE"
    CERT=$(mktemp /tmp/temporary-file.XXXXXXXX)
    openssl s_client -showcerts -connect $BOSH_HOST:443 </dev/null 2>/dev/null|openssl x509 -outform PEM > $CERT
    keytool -import -noprompt -alias $BOSH_HOST -file $CERT -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PWD"
fi

# Create credentials file
i=0
while [ "$i" -lt "${USERS}" ]
do
  echo "${ROOM}_${i}:${PASSWORD}" >> ${CREDENTIALS}
  i=`expr $i + 1`
done

# run jar
exec java -Djavax.net.ssl.keyStore=${KEYSTORE_FILE} \
  -Djavax.net.ssl.keyStorePassword=${KEYSTORE_PWD} \
  -Djavax.net.ssl.trustStore=${KEYSTORE_FILE} \
  -Djavax.net.ssl.trustStorePassword=${KEYSTORE_PWD} \
  -Djava.library.path=/jitsi-hammer/lib \
  -Dnet.java.sip.communicator.SC_HOME_DIR_LOCATION=/ \
  -Dnet.java.sip.communicator.SC_HOME_DIR_NAME=.jitsi-hammer \
  -Djava.util.logging.config.file=/jitsi-hammer/lib/logging.properties \
  -jar /jitsi-hammer/jitsi-hammer-1.0-SNAPSHOT.jar \
  -audiortpdump /jitsi-hammer/resources/${AUDIO_RTP_DUMP} \
  -videortpdump /jitsi-hammer/resources/${VIDEO_RTP_DUMP} \
  -length ${LENGTH} \
  -room ${ROOM} \
  -XMPPdomain ${XMPP_DOMAIN} \
  -focusJID focus.${XMPP_DOMAIN} \
  -MUCdomain conference.${XMPP_DOMAIN} \
  -credentials ${CREDENTIALS} \
  -overallstats \
  -allstats \
  -interval ${INTERVAL} \
  -u "https://${BOSH_HOST}/http-bind"
