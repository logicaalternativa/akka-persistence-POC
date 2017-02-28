# Akka-persistence with CQRS POC (Proof of concept)

# Functional aproach

## Dependency 
Cassandra is needed to execute the POC

## Install

```
  sbt pack
```

## Execute

There is only one application: **AppCQRS**. It is writen orders (_Order_ is the 'write' concept), the Querys (it is representated by *User* concept).

It is used a functional approach, where the business logic is isolated from side effects. There are tree service. **OrderService** with every operactions about _orders_, **PublishService** with the publishing events logic, and the **UserViewService** related the view user _querys_. 

Every funcitonal programs is included in **AplicationFlow**

The akka cluster sharding y akka persistence is only used as final implementation of the services and they do not have business logic.

### Clustering

It will be able to add several nodes (no seeder nodes) if it is changed the port (different to 2551, 2552 or 2553) 

### Execution example:

```
 java -cp 'target/pack/lib/*' -Dclustering.port=2551  poc.persistence.AppCQRS
```

### Logging

Every application writes its log in the file _log/debug-${**clustering.port**}.log_ 


