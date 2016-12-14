Use *ccm* to start a Cassandra Cluster
======================================

Get *ccm* from https://github.com/pcmanus/ccm

On Mac OS X you may need to:
```
sudo ifconfig lo0 alias 127.0.0.2 up
sudo ifconfig lo0 alias 127.0.0.3 up
```

Create a *test* cluster

``` ccm create test -v 3.0.8 -n 3 -s ```
