package facebookProject4

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.routing.RoundRobinRouter
import scala.collection.mutable._
import spray.routing.SimpleRoutingApp
import scala.util.Random
import java.io._

//spray stuff
import akka.routing.ConsistentHashingRouter
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.actor._
import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout
//import common._
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.ArrayList
import scala.collection.mutable.MutableList
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
//spray client stuff
import akka.actor._
import scala.concurrent._
import akka.pattern.ask
import spray.routing.SimpleRoutingApp

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write,writePretty}
import spray.http._
import spray.http.MediaTypes._
import spray.routing.{Route, RequestContext}
import spray.routing.directives._
import scala.concurrent.duration._
//import common._
import scala.util.Random


case class Profile(userName : String,ageOfUser: Int) extends Serializable

case class SetProfileInfoOfUser(userCount: Int)
case class GetProfileInfoOfUser(userName:String)

object Server_Part extends App with SimpleRoutingApp
{ 
	var tweetsHandled:Long=0
  override def main(args: Array[String]) 
  {
    //import system.dispatcher
	  implicit val system = ActorSystem("facebookAPI")
	  val actorCount: Int = Runtime.getRuntime().availableProcessors()*100
	  var tweetStore = new ListBuffer[Queue[String]] ()
	  var hashTagStore = new ListBuffer[Queue[String]] ()
	  var followingList = new ArrayBuffer[ArrayBuffer[Int]] ()
    implicit val timeout = akka.util.Timeout(500)

	  println("Twitter Server Started....")

    val server_actor = system.actorOf(Props[Server_Part], name="server_part")

    lazy val createUserForFb = post {
          path("facebook" / "createUser") {
            println("bp1....")
            parameters("userCount".as[Int]) { (userCount) =>
            val server_actor = system.actorOf(Props[Server_Part],name="facebookUser"+userCount) 
            println(server_actor)
            server_actor!SetProfileInfoOfUser(userCount)
              complete {
                "rank of users="+userCount
              }
            }
          }
        }

        lazy val profileInfoOfUserOnFb = get {
        respondWithMediaType(MediaTypes.`application/json`)
              path("facebook" / "getProfileInfoOfUser"){
                parameters("userName".as[String]) { (userName) =>
                  val actor = system.actorSelection("akka://facebookAPI/user/"+userName)
                  var future = actor ? GetProfileInfoOfUser(userName)
                  var userProfile = Await.result(future, timeout.duration).asInstanceOf[Profile]
                  complete{ 
                  JsonUtil.toJson(userProfile)//change it
                  }
                }
                
              }
            }

  	   startServer(interface = "localhost", port = 8080) {
        
        createUserForFb ~
        profileInfoOfUserOnFb
        
         }


      }       
	//}
	  
  //this class actually denotes the user actor of facebook
  class Server_Part extends Actor 
  {
    val writer = new FileWriter("Server_Output.txt",true )
    var tweetsMap = new scala.collection.mutable.HashMap[String, Profile]()
    var userName:String = ""
    var ageOfUser:Int = 0
    //var profileObj : Profile = Profile("NoName",0)
    //var remote = context.actorFor("akka.tcp://ServerSystem@127.0.0.1:5152/user/Tweeting")
      def receive = 
      {    
        //FOLLOWING LIST AND TWEETSTORE
        case SetProfileInfoOfUser(userCount:Int)=>
          {    
          println("bp2....")
          userName = "facebookUser"+userCount;
          ageOfUser = userCount
          println("Username is" + userName);
          println("ageOfUser is" + ageOfUser);
          val profileObj = Profile(userName,ageOfUser)
          putProfile(userName,profileObj)
          }

      case GetProfileInfoOfUser(userName:String)=>
          { 
          val profileObject = tweetsMap.get(userName) match{
          case Some(profileObject) => profileObject
          case None => Profile("Error",0)
         }
      
          //sender ! profileObject
          //println("Username is" + userName);
          //println("ageOfUser is" + ageOfUser);
          println("yay")
          sender ! profileObject
          }      


      }

      def putProfile(userName:String,profileObj:Profile){
        tweetsMap += (userName -> profileObj)
      }
    
  }

}

object JsonUtil{
  
  //private implicit val formats = Serialization.formats(NoTypeHints)
  implicit val formats = native.Serialization.formats(ShortTypeHints(List(classOf[Profile])))
  def toJson(profile:Profile) : String = writePretty(profile)
  
  
  
}