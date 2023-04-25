FROM mysql:8

ENV MYSQL_ROOT_PASSWORD=admin
ENV MYSQL_DATABASE=monitoring_db

ADD monitoring_db.sql /docker-entrypoint-initdb.d
