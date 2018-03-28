# --- !Ups

create table "products" (
  "name" VARCHAR NOT NULL,
  "cached_price" DECIMAL(21,2) NOT NULL,
  "photo" VARCHAR,
  "cached_average_rating" DECIMAL(21,2),
  "description" VARCHAR NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY
);

create table "reviews" (
  "author" VARCHAR NOT NULL,
  "content" VARCHAR NOT NULL,
  "rating" DECIMAL(21,2),
  "date" TIMESTAMP,
  "product_id" BIGINT NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  foreign key ("product_id") references "products"("id")
);

create table "prices" (
  "product_id" BIGINT NOT NULL,
  "price" DECIMAL(21,2) NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  foreign key ("product_id") references "products"("id")
);


create table "availability" (
  "product_id" BIGINT NOT NULL,
  "stock" BIGINT NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  FOREIGN KEY ("product_id") REFERENCES "products"("id")
);

create table "users" (
  "provider_id" VARCHAR NOT NULL,
  "provider_key" VARCHAR NOT NULL,
  "first_name" VARCHAR,
  "last_name" VARCHAR,
  "email" VARCHAR NOT NULL,
  "id" UUID NOT NULL PRIMARY KEY
);

create table "auth_tokens" (
  "user_id" UUID NOT NULL,
  "expiry" TIMESTAMP NOT NULL,
  "id" UUID NOT NULL PRIMARY KEY,
  FOREIGN KEY ("user_id") REFERENCES "users"("id")
);

create table "passwords" (
  "provider_id" VARCHAR NOT NULL,
  "provider_key" VARCHAR NOT NULL,
  "hasher" VARCHAR NOT NULL,
  "password" VARCHAR NOT NULL,
  "salt" VARCHAR,
  PRIMARY KEY ("provider_id", "provider_key")
);

create table "orders" (
  "status" INT NOT NULL,
  "user_id" UUID NOT NULL,
  "date" TIMESTAMP NOT NULL,
  "full_name" VARCHAR NOT NULL,
  "street_address" VARCHAR NOT NULL,
  "zip_code" VARCHAR NOT NULL,
  "city" VARCHAR NOT NULL,
  "country" VARCHAR NOT NULL,
  "id" UUID NOT NULL PRIMARY KEY,
  FOREIGN KEY ("user_id") REFERENCES "users"("id")
);

create table "line_items" (
  "order_id" UUID NOT NULL,
  "product_id" BIGINT NOT NULL,
  "amount" INT NOT NULL,
  "price" DECIMAL(21,2) NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  foreign key ("product_id") references "products"("id"),
  foreign key ("order_id") references "orders"("id")
);

create table "cart_items" (
  "product_id" BIGINT NOT NULL,
  "amount" BIGINT NOT NULL,
  "user_id" UUID NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  foreign key ("product_id") references "products"("id"),
  foreign key ("user_id") REFERENCES "users"("id")
);

create table "payments" (
  "total_price" DECIMAL(21,2) NOT NULL,
  "email" VARCHAR NOT NULL,
  "address" VARCHAR NOT NULL,
  "return_url" VARCHAR NOT NULL,
  "is_paid" BOOLEAN NOT NULL,
  "id" UUID NOT NULL PRIMARY KEY
);


create table "payment_products" (
  "name" VARCHAR NOT NULL,
  "unit_price" DECIMAL(21,2) NOT NULL,
  "amount" BIGINT NOT NULL,
  "payment_id" UUID NOT NULL,
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  FOREIGN KEY ("payment_id") REFERENCES "payments"("id")
);

create index product_search_index on "products" ("cached_average_rating", "name");

# --- !Downs

drop index product_search_index;
drop table "payment_products";
drop table "payments";
drop table "cart_items";
drop table "line_items";
drop table "orders";
drop table "passwords";
drop table "auth_tokens";
drop table "users";
drop table "availability";
drop table "prices";
drop table "reviews";
drop table "products";
