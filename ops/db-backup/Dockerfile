FROM postgres:16-alpine

RUN apk add --no-cache bash aws-cli coreutils ca-certificates

WORKDIR /app
COPY backup.sh /app/backup.sh

RUN chmod +x /app/backup.sh

ENTRYPOINT ["/app/backup.sh"]
