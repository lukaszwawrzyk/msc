# --- !Ups

create table "offsets" (
  "name"  VARCHAR NOT NULL PRIMARY KEY,
  "value" UUID
);

# --- !Downs

drop table "offsets";
