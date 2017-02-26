package poc.persistence.stream


object StreamToQueryFunctional {
  
  import akka.actor.{ActorSystem,ActorRef}
  import akka.stream.{OverflowStrategy, ActorMaterializer, Graph}
  import akka.stream.scaladsl.{Sink, Flow, Source}
  import org.slf4j.{Logger,LoggerFactory}
  import poc.persistence.functional.ApplicationFlow
  import poc.persistence.functional.publish.PublishService
  import poc.persistence.functional.read.UserViewService
  import poc.persistence.functional.read.UserViewService
  import poc.persistence.functional.write.OrderService
  import poc.persistence.functional.Error
  import poc.persistence.publish.OrderEventWithDeliver
  import scalaz.{Monad,MonadError}
  import scalaz.syntax.monadError._
  
  val logger = LoggerFactory.getLogger( getClass )
  
  def actorStream[P[_]]( implicit system : ActorSystem,
                                  PS: PublishService[P],
                                  US: UserViewService[P],
                                  M : Monad[P],
                                  E : MonadError[P,Error]
                                  ) : ActorRef = {
      
    import akka.stream.{ClosedShape,Inlet,FlowShape,Outlet,SinkShape}
    import akka.stream.scaladsl.{RunnableGraph,GraphDSL,Broadcast,Zip,Merge}
    import GraphDSL.Implicits._
    
    
    implicit val materializer = ActorMaterializer()
    
    val source = Source.actorRef[OrderEventWithDeliver]( Int.MaxValue, OverflowStrategy.fail )
    
    source.named( "stream-cqrs" )
    
    //~ val sink  = Sink.foreach(println)
    val sink  = Sink.ignore
    
    val flow = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      
      type OE = OrderEventWithDeliver
   
      // prepare graph elements
      val broadcast = b.add(Broadcast[OE](2))
      //~ val zip = b.add( Zip[OE, OE] )
      val map: FlowShape[OE, OE] = b.add( Flow[OE].map{ 
        
            s => {
              val res = ApplicationFlow.toView( s )
              s
            } 
        
        } )
      val end = b.add(Sink.ignore)
   
      // connect the graph
      broadcast.out(0).log( "StreamToQuery1", s  => logger.info ( "Logger 1 processing, element -> {}", s ) ) ~> end.in
      broadcast.out(1) ~> map.in
         
   
      // expose ports
      FlowShape(broadcast.in, map.out)
    })
    
     
    flow.runWith(source, sink)._1
  }
  
}



