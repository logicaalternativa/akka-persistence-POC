# Akka-persistence with CQRS POC (Proof of concept)

## Dependency 
Cassandra is needed to execute the POC

## Install

```
  sbt pack
```

## Execute

There are three applications. One of them writes orders (_Order_ concept), the second proccesses every is the Query application (it is representated by *User* concept).

In this aproach, every **Order** (Write) actor has a dependency to an special actor called "StreamActor". Every persisted event is sent to this ator. Behind the scenes, this actor is an akka stream that allows a pipe with User Actor (Query).

In order to avoid a loss of data. The order actor implements the "_“at-most-once” _" message pattern.

They could be run separately or all at once. It will be able to add other nodes (no seeder nodes) if it is changed the port (different to 2551, 2552 or 2553) 

### Write app

Execution example:

```
 java -cp 'target/pack/lib/*' -Dclustering.port=2551  poc.persistence.write.WriteApp
```

### Query app

Execution example:

```
 java -cp 'target/pack/lib/*' -Dclustering.port=2553 poc.persistence.read.QueryApp
```

Every application writes its log in the file _log/debug-${**clustering.port**}.log_ 


