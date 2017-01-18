package poc.persistence



trait BaseObjectActor  {
  
  import akka.actor.{ActorSystem, ActorRef, Props}
  import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}

  def name : String
  
  protected def extractShardId : ShardRegion.ExtractShardId
  protected def extractEntityId : ShardRegion.ExtractEntityId 
  protected def props( implicit system : ActorSystem ) : Props
  
  
  def startRegion( implicit system: ActorSystem ) :  ActorRef = {
    
    ClusterSharding(system).start(
      typeName = name,
      entityProps = props( system ),
      settings = ClusterShardingSettings(system),
      extractShardId = extractShardId,
      extractEntityId = extractEntityId
    )
    
  }
  
  def receiver( implicit system: ActorSystem ) : ActorRef = {
    
    ClusterSharding(system).shardRegion( name )
    
  }
  
}



