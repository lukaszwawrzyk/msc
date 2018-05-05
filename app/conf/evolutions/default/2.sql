# --- !Ups

create table "offsets" (
  "name"  VARCHAR NOT NULL PRIMARY KEY,
  "value" BIGINT NOT NULL
);

# --- !Downs

drop table "offsets";
