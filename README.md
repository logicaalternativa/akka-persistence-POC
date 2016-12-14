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

Now run the read side of the application.

