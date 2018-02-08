# --- !Ups

create table "people" (
  "id" bigint generated by default as identity(start with 1) not null primary key,
  "name" varchar not null,
  "age" int not null
);

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
  "product_id" BIGINT NOT NULL,
  "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  foreign key ("product_id") references "products"("id")
);

create table "prices" (
  "product_id" BIGINT NOT NULL,
  "price" DECIMAL(21,2) NOT NULL,
  "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  foreign key ("product_id") references "products"("id")
)

# --- !Downs

drop table "people";
drop table "products";
drop table "reviews";
drop table "prices";