package FacebookAPI

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
case class Send_createUser(userCount: Int)
case class Send_getUser(userCount: Int)
case object Send_getUsersInfo

//case class send(user: Int)
//case class stopChk(start: Long)

object FacebookClient 
{
    private val start:Long=System.currentTimeMillis
    def main(args: Array[String])
  {
	  val system = ActorSystem("ClientSystem")
	  //println("How many Users?")
	  val numOfUsers = 5
	  val client_actor =system.actorOf(Props(new FacebookAPISimulator(system,numOfUsers)),name="FacebookAPISimulator")
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
  			val client_driver = context.actorOf(Props(new FacebookAPIClient(system)),name="FacebookAPIClient") 					
  			for(i <-0 until userCount) 
  			{
          println("chutiyapa")
  			 client_driver ! Send_createUser(i)
  			}

        client_driver ! Send_getUser(3)

        client_driver ! Send_getUsersInfo
        }
  	}
}
//usercount is nth number of facebook user
class FacebookAPIClient(system:ActorSystem) extends Actor { 
  import system.dispatcher
  val pipeline1 = sendReceive
  val pipeline2 = sendReceive
  val pipeline3 = sendReceive

	def receive = 
  	{
  		case Send_createUser(userCount) =>
  		{
          println("bpc1....")
            pipeline1(Post("http://localhost:8080/facebook/createUser?userCount="+userCount))
  		}

      case Send_getUser(userCount) =>
      {
           val result =  pipeline2(Get("http://localhost:8080/facebook/getProfileInfoOfUser?userName=facebookUser"+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
          }
      }

      case Send_getUsersInfo =>
      {
        println("Send_getUsersInfo")
        val result = pipeline3(Get("http://localhost:8080/facebook/getProfileInfoOfUsers"))
        result.foreach { response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        }
      }

  	}
}
//}
	







