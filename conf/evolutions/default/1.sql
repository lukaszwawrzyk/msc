# --- !Ups

create table "products" (
  "name" VARCHAR NOT NULL,
  "cached_price" DECIMAL(21,2) NOT NULL,
  "photo" VARCHAR,
  "cached_average_rating" DOUBLE,
  "description" VARCHAR NOT NULL,
  "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT
);

create table "reviews" (
  "author" VARCHAR NOT NULL,
  "content" VARCHAR NOT NULL,
  "rating" DOUBLE,
  "date" TIMESTAMP,
  "product_id" BIGINT NOT NULL,
  "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  foreign key ("product_id") references "products"("id")
);

create table "prices" (
  "product_id" BIGINT NOT NULL,
  "price" DECIMAL(21,2) NOT NULL,
  "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  foreign key ("product_id") references "products"("id")
);


create table "availability" (
  "product_id" BIGINT NOT NULL,
  "stock" BIGINT NOT NULL,
  "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  FOREIGN KEY ("product_id") REFERENCES "products"("id")
);

create table "users" (
  "provider_id" VARCHAR NOT NULL,
  "provider_key" VARCHAR NOT NULL,
  "first_name" VARCHAR,
  "last_name" VARCHAR,
  "email" VARCHAR NOT NULL,
  "id" VARCHAR(32) NOT NULL PRIMARY KEY
);

create table "auth_tokens" (
  "user_id" VARCHAR(32) NOT NULL,
  "expiry" TIMESTAMP NOT NULL ,
  "id" VARCHAR(32) NOT NULL PRIMARY KEY,
  FOREIGN KEY ("user_id") REFERENCES "users"("id")
);

# --- !Downs

drop table "auth_tokens";
drop table "users";
drop table "availability";
drop table "prices";
drop table "reviews";
drop table "products";
