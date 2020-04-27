# Database Operations Offering an Alternative to Stroom UI
Sometimes it is necessary or desirable to create Stroom content or perform another Stroom operation 
without using the Stroom UI, e.g. in order to save time in a test environment, or because the UI is not running.

This page explains how certain tasks can be achieved without the Stroom UI.

## Accessing the database
Stroom content exists in the database.  

Assuming that mysql is running in a docker container on localhost, it is possible to connect to it using the following command:

```shell script
docker exec -it stroom-all-dbs mysql -h"localhost" -P"3307" -u"stroomuser" -p"stroompassword1" stroom
```

Replacing the password as necessary for your environment.

Alternatively clone the `stroom-resources` repo and `source .aliases` before using the alias `stroomdb` for same effect.

## Creating Index Volume and Index Volume Group "Test Group"
As required by the examples
```SQL
insert into index_volume_group set version=1,create_time_ms=1582286232829,create_user='INTERNAL_PROCESSING_USER',update_time_ms=1582286232829,update_user='INTERNAL_PROCESSING_USER',name='Test Group';
insert into index_volume set version=1,create_time_ms=1582286232829,create_user='INTERNAL_PROCESSING_USER',update_time_ms=1582286232829,update_user='INTERNAL_PROCESSING_USER',node_name='node1a',path='/tmp/stroom/analyticdemo/indexvols/group1',index_volume_group_name='Test Group',state=0,bytes_limit=20000000,bytes_used=0,bytes_free=20000000,bytes_total=20000000,status_ms=1582286232829;
```

## Enabling all processors and processor filters

***This will enable all processors and processor filters. Only do this on a standalone/test instance of stroom!***
```SQL
update processor set enabled = true;
update processor_filter set enabled = true;
```

## Enabling a specific stream processor and processor filter
***Important!***  The id of the processor and processor filter (`11` and `41` in the example, below) must first be determined, 
either by querying the database directly or by using the Stroom UI.

```SQL
update processor set enabled = true where id=11;
update processor_filter set enabled = true where id=41;
```

## Enabling the admin user
If it somehow becomes locked!

Note: The relevant table resides in the authetication service's database, rather than the main stroom database.

Clone the `stroom-resources` repo and `source .aliases` before using the alias `authdb` to access the mysql CLI.

```SQL
update user set state='enabled',login_failures=0 where email='admin';
```





