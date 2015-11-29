package FacebookAPI

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import scala.collection.mutable._
import spray.http._
import spray.client.pipelining._
import scala.util.Random
import scala.concurrent.ExecutionContext
import akka.actor._
import akka.util._
import scala.concurrent.duration._
import akka.routing.RoundRobinRouter
import java.net.InetAddress

//import scala.concurrent.ExecutionContext.Implicits.global


case class Start(system : ActorSystem)
case class Send_createUser(userCount: String , dob:String, gender:String, phoneNumber:String)
case class Send_getUser(userCount: Int)
case class Send_getAllUsers(userCount:Int)
case class Send_getAllPosts(userCount:Int)
case class Send_updateFriendListOfFbUser(userName:Int,friendUserName:Int,action:String)

case class Send_createPost(userCount:String,content:String,postId:String)
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
  			var gender :  String = ""
        for(i <-0 until userCount) 
  			{
          //println("chutiyapa")
          val rnd = new scala.util.Random
          val dd = 1 + rnd.nextInt(28)
          val mm = 1 + rnd.nextInt(12)
          val yy = 1970 + rnd.nextInt(36)
          val dob : String = mm.toString+"-"+dd.toString+"-"+yy.toString
          if(i%2==0){
            gender = "F"
          }
          else{
            gender = "M"
            } 
          val areaCode = 300 + rnd.nextInt(700)
          val firstPart = 300 + rnd.nextInt(700)
          val secondPart = 1000 + rnd.nextInt(9000)
          val phoneNumber : String = "("+areaCode.toString+")"+" "+firstPart.toString+"-"+secondPart.toString
  			  client_driver ! Send_createUser(i.toString,dob,gender,phoneNumber)
  			}


        //client_driver ! Send_getUser(2)

        //client_driver ! Send_getAllUsers(3)


        //client_driver ! Send_updateFriendListOfFbUser(1,3,"connect")
        //client_driver ! Send_updateFriendListOfFbUser(1,4,"connect")


        //post creation apis - do not delete them
        // client_driver ! Send_createPost("3","First post of the User","1")
        // client_driver ! Send_createPost("3","second post of the User","2")
        // client_driver ! Send_createPost("2","first post of the thh User","3")
        // client_driver ! Send_createPost("2","second post of the thh User","4")

        // client_driver ! Send_getAllPosts(3)


        }
  	}
}
//usercount is nth number of facebook user
class FacebookAPIClient(system:ActorSystem) extends Actor { 
  import system.dispatcher
  val pipeline1 = sendReceive
  val pipeline2 = sendReceive
  val pipeline3 = sendReceive
  val pipeline4 = sendReceive
  val pipeline5 = sendReceive

	def receive = 
  	{
  		case Send_createUser(userCount,dob,gender,phoneNumber) =>
      {
          println("bpc1....")
          pipeline1(Post("http://localhost:8080/facebook/createUser",FormData(Seq("field1"->userCount, "field2"->dob, "field3"->gender, "field4"->phoneNumber))))
      }

      case Send_createPost(userCount,content,postId) =>
      {
          println("bpc6....")
          pipeline1(Post("http://localhost:8080/facebook/createPost",FormData(Seq("field1"->userCount, "field2"->content,"field3"->postId))))
      }


      case Send_updateFriendListOfFbUser(userName,friendUserName,action) =>
      {
          println("bpc3....")
          pipeline1(Post("http://localhost:8080/facebook/updateFriendListOfFbUser?userName=facebookUser"+userName+"&friendUserName=facebookUser"+friendUserName+"&action="+action ))
      }

      case Send_getUser(userCount) =>
      {
           val result =  pipeline2(Get("http://localhost:8080/facebook/getProfileInfoOfUser/"+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
          }
      }

      case Send_getAllUsers(userCount) =>
      {
           val result =  pipeline2(Get("http://localhost:8080/facebook/getProfileOfAllFacebookUsers?start="+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           //println(s"Request completed with status ${response.status} and content:\n${response.asString}")
          }
      }

      case Send_getAllPosts(userCount) =>
      {
           val result =  pipeline5(Get("http://localhost:8080/facebook/getPostsOfAllFacebookUsers?start="+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           //println(s"Request completed with status ${response.status} and content:\n${response.asString}")
          }
      }

  	}
}
//}
	








