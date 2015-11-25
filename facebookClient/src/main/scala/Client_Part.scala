package Twitter_Simulator

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import scala.collection.mutable._
import spray.http._
import spray.client.pipelining._
import scala.util.Random
import scala.concurrent.ExecutionContext
//client stuff
import akka.actor._
//import common._
import akka.util._
import scala.concurrent.duration._
import akka.routing.RoundRobinRouter
import java.net.InetAddress

//import scala.concurrent.ExecutionContext.Implicits.global


case class Start(system : ActorSystem)
case class send_createUser(userCount: Int)
case class send_getUser(userCount: Int)

case class send(user: Int)
case class stopChk(start: Long)

object FacebookClient 
{
    private val start:Long=System.currentTimeMillis
    def main(args: Array[String])
  {
	  val system = ActorSystem("ClientSystem")
	  //println("How many Users?")
	  val numOfUsers = 5
	  val client_actor =system.actorOf(Props(new FacebookAPISimulator(system,numOfUsers)),name="ClientActor")
	 // val receiver =system.actorOf(Props(new clientReceiver()),name="ClientReceiver")
	  client_actor ! Start(system)
  }
}

//simulatorn, use statistics , creation and posting , reading according to our studies, with n number of users
class FacebookAPISimulator(system : ActorSystem, userCount : Int) extends Actor 
{
  var clientBuffer= new ArrayBuffer[ActorRef]() 
  
	def receive = 
  	{       
  	 case Start(system) => 
  		{
  			val client_driver = context.actorOf(Props(new FacebookAPIClient(system)),name="TwitterTable") 					
  			for(i <-0 until userCount) 
  			{
          println("chutiyapa")
  			 client_driver ! send_createUser(i)
  			}

        client_driver ! send_getUser(3)
        }
  	}
}
//usercount is nth number of facebook user
class FacebookAPIClient(system:ActorSystem) extends Actor { 
  import system.dispatcher
  val pipeline1 = sendReceive
  val pipeline2 = sendReceive

	def receive = 
  	{
  		case send_createUser(userCount) =>
  		{
          println("bpc1....")
            pipeline1(Post("http://localhost:8080/facebook/createUser?userCount="+userCount))
  		}

      case send_getUser(userCount) =>
      {
           val result =  pipeline2(Get("http://localhost:8080/facebook/getProfileInfoOfUser?userName=facebookUser"+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
          }
      }

  	}
}
//}
	








