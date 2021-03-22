[Trino](https://trino.io/) is a distributed SQL engine, which reads (and writes) data directly from their sources. [My tech talk](https://github.com/mat3e/talks/tree/master/docs/sql).

# Trino demo
This project uses Quarkus and JDBC to connect with Trino.

## 1. MySQL database:
```
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret mysql:5
```
```sql
create schema todos;

create table task_groups
(
  id int auto_increment primary key,
  description varchar(100) not null,
  done bit null
);

create table tasks
(
  id int auto_increment primary key,
  description varchar(100) not null,
  done bit null,
  deadline datetime null,
  created_on datetime null,
  updated_on datetime null,
  task_group_id int null,
  assigned_user int null,
  constraint tasks_ibfk_1 foreign key (task_group_id) references task_groups (id)
);

INSERT INTO task_groups (id, description, done) VALUES (1, 'Job tasks', false);

INSERT INTO tasks (id, description, done, deadline, created_on, updated_on, task_group_id, assigned_user) VALUES (1, 'Learn Java migrations', true, null, null, null, null, 1);
INSERT INTO tasks (id, description, done, deadline, created_on, updated_on, task_group_id, assigned_user) VALUES (2, 'Start job', false, null, '2021-03-18 16:42:00', null, 1, 3);
INSERT INTO tasks (id, description, done, deadline, created_on, updated_on, task_group_id, assigned_user) VALUES (3, 'Question everything', true, null, '2021-03-18 16:42:00', null, 1, 2);
INSERT INTO tasks (id, description, done, deadline, created_on, updated_on, task_group_id, assigned_user) VALUES (4, 'End job', false, null, '2021-03-18 16:42:00', null, 1, 2);
INSERT INTO tasks (id, description, done, deadline, created_on, updated_on, task_group_id, assigned_user) VALUES (5, 'Test that', false, null, '2021-03-18 16:42:00', null, null, 1);
```

## 2. Mongo:
```
docker run -d -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=secret mongo
```
```javascript
use userdb

db.users.insertOne({id: 1, login: 'admin', mail: 'admin@ad.min', roles: ['ADMIN']})
db.users.insertOne({id: 2, login: 'user', mail: 'user@ad.min', roles: ['USER']})
db.users.insertOne({id: 3, login: 'test', mail: 'test@ad.min', roles: ['USER', 'MODERATOR']})
```

## 3. Google Sheets:
* Metadata (`tables` sheet):
  
  Table Name|sheetid#sheetname|Owner|Notes
  ---|---|---|---
  metadata_table|sheet_id#tables| |Self reference to this sheet as table
  users|sheet_id#users| |Next sheet here
* Users (`users` sheet):
 
  Number|Name|Surname
  ---|---|---
  1|Anna|Nowak
  2|Jan|Kowalski
* [Enable Google Sheets API](https://console.developers.google.com/apis/library/sheets.googleapis.com)
* Create Credentials (Service Account)
* Create a new JSON key for account
* Share your sheet(s) with account's e-mail

## 4. Trino:
* Download & unpack [trino-server-353.tar.gz](https://repo1.maven.org/maven2/io/trino/trino-server/353/trino-server-353.tar.gz)
* Create `etc` directory inside the installation directory and `catalog` subdirectory in `etc`
* `etc/node.properties`
  ```
  node.environment=common_for_all_the_nodes
  node.id=consistent_but_unique_node1
  node.data-dir=/path/to/store/logs/data
  ```
* `etc/jvm.config`
  ```
  -server
  -Xmx16G
  -XX:-UseBiasedLocking
  -XX:+UseG1GC
  -XX:G1HeapRegionSize=32M
  -XX:+ExplicitGCInvokesConcurrent
  -XX:+ExitOnOutOfMemoryError
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:ReservedCodeCacheSize=512M
  -XX:PerMethodRecompilationCutoff=10000
  -XX:PerBytecodeRecompilationCutoff=10000
  -Djdk.attach.allowAttachSelf=true
  -Djdk.nio.maxCachedBufferSize=2000000
  ```
* `etc/config.properties`
  ```
  coordinator=true
  node-scheduler.include-coordinator=true
  http-server.http.port=9090
  query.max-memory=5GB
  query.max-memory-per-node=1GB
  query.max-total-memory-per-node=2GB
  discovery-server.enabled=true
  discovery.uri=http://localhost:9090
  ```
* `etc/catalog/mysql.properties`
  ```
  connector.name=mysql
  connection-url=jdbc:mysql://localhost:3306
  connection-user=root
  connection-password=secret
  ```
* `etc/catalog/mongo.properties`
  ```
  connector.name=mongodb
  mongodb.seeds=localhost:27017
  mongodb.credentials=root:secret@admin
  ```
* `etc/catalog/sheets.properties`
  ```
  connector.name=gsheets
  credentials-path=/path/to/json/key
  metadata-sheet-id=sheet-id-with-metadata
  ```
* Execute `bin/launcher run`

## 5. Start the app `./mvnw compile quarkus:dev`, open `localhost:8080`

### Reference:
* [Quarkus guide](https://quarkus.io/guides/getting-started)
* [Trino docs](https://trino.io/docs/current/index.html)
  * [Configuring Trino](https://trino.io/docs/current/installation/deployment.html)
  * [Configuring MySQL connector](https://trino.io/docs/current/connector/mysql.html)
  * [Configuring Mongo connector](https://trino.io/docs/current/connector/mongodb.html)
  * [Configuring Google Sheets connector](https://trino.io/docs/current/connector/googlesheets.html)
