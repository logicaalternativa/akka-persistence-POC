# Akka-persistence with CQRS POC (Proof of concept)

## Dependency 
Cassandra is needed to execute the POC

## Install

```
  sbt pack
```

## Execute

There are three applications. One of them writes orders (_Order_ concept), the second proccesses every is the Query application (it is representated by *User* concept).

In this aproach, every **Order** (_Write_) actor has a dependency to an special actor called "_StreamActor_". Every persisted event is sent to this actor. Behind the scenes, this actor is an akka stream that allows a pipe with User **Actor** (_Query_).

In order to avoid a loss of data. The order actor implements the "_at-most-once_" message pattern.

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


