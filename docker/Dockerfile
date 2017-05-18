FROM openjdk:8-jre-alpine

RUN apk update && apk add openssl

COPY ./jitsi-hammer-1.0-SNAPSHOT-linux-x64-bin-archive.zip /

ENV AUDIO_RTP_DUMP="hammer-opus.rtpdump" \
    VIDEO_RTP_DUMP="rtp_vp8.rtpdump" \
    CREDENTIALS="/credentials.txt" \
    LENGTH=-1 \
    INTERVAL=2000 \
    ROOM="hammertest" \
    PASSWORD="" \
    XMPP_DOMAIN="example.com" \
    KEYSTORE_FILE="/keystore.ks" \
    KEYSTORE_PWD=123456 \
    USERS=2 \
    BOSH_HOST="example.com"

RUN unzip /jitsi-hammer-1.0-SNAPSHOT-linux-x64-bin-archive.zip && \
  rm /jitsi-hammer-1.0-SNAPSHOT-linux-x64-bin-archive.zip && \
  mv jitsi-hammer-linux-x64-1.0-SNAPSHOT jitsi-hammer

CMD ["/jitsi-hammer/hammer.sh"]

