# Akka-persistence with CQRS POC (Proof of concept)

## Dependency 
Cassandra is needed to execute the POC

## Install

```
  sbt pack
```

## Execute

There are three applications. One of them writes *orders*, the second proccesses every event and sends this data to User. The third is the Query application (it is representated by *User* concept).

They could be run separately or all at once. It will be able to add other nodes (no seeder nodes) if it is changed the port (different to 2551, 2552 or 2553) 

### Write app

Execution example:

```
 java -cp 'target/pack/lib/*' -Dclustering.port=2551  poc.persistence.write.WriteApp
```

### Stream app

Execution example:

```
 java -cp 'target/pack/lib/*' -Dclustering.port=2552  poc.persistence.read.ReadApp
```

### Query app

Execution example:

```
 java -cp 'target/pack/lib/*' -Dclustering.port=2553 poc.persistence.read.QueryApp
```

Every application writes its log in the file _log/debug-${**clustering.port**}.log_ 


