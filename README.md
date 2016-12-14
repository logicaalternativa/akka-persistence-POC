Use *ccm* to start a Cassandra Cluster
======================================

Get *ccm* from https://github.com/pcmanus/ccm. Follow the instructions to install.

On Mac OS X you may need to:
```
sudo ifconfig lo0 alias 127.0.0.2 up
sudo ifconfig lo0 alias 127.0.0.3 up
```

Create a *test* cluster:

``` ccm create test -v 3.0.8 -n 3 -s ```

Run the command side of the application.

Initialize an order:

```
curl -H "Content-Type: application/json" -X POST http://localhost:8080/order/initialize -d '{"idOrder":"1", "idUser": 42}'
```

Cancel the order:

```
 curl -H "Content-Type: application/json" -X POST http://localhost:8080/order/cancel -d '{"idOrder":"1", "idUser": 42}'
```

Note that if you try to cancel the order again, you'll get a rejection message.

```
{"message":"command rejected"}
```

Connect to Cassandra using *cqlsh*.

Issue the following commands:

```
$ ccm node1 cqlsh
Connected to test at 127.0.0.1:9042.
[cqlsh 5.0.1 | Cassandra 3.0.8 | CQL spec 3.4.0 | Native protocol v4]
Use HELP for help.
cqlsh> USE akka;
cqlsh:akka> SELECT persistence_id, sequence_nr, ser_manifest, blobastext(event) AS event FROM messages;

 persistence_id | sequence_nr | ser_manifest                            | event
----------------+-------------+-----------------------------------------+-----------------------------
              1 |           1 | poc.persistence.events.OrderInitialized | {"idOrder":"1","idUser":42}
              1 |           2 |   poc.persistence.events.OrderCancelled | {"idOrder":"1","idUser":42}

(2 rows)
cqlsh:akka>
```

Now run the read side of the application.

