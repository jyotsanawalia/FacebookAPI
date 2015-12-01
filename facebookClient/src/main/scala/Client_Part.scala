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

import java.io.{File,FileInputStream,FileOutputStream}
import java.io.FileWriter;
//import scala.concurrent.ExecutionContext.Implicits.global


case class Start(system : ActorSystem)
case class Send_createUser(userCount: String , dob:String, gender:String, phoneNumber:String)
case class Send_getUser(userCount: Int)
case class Send_getAllUsers(userCount:Int)
case class Send_getAllPosts(userCount:Int)
case class Send_updateFriendListOfFbUser(userName:String,friendUserName:String,action:String)
case class Send_createPost(userCount:String,content:String,postId:String)

case class Send_createAlbum(userCount:String,imageContent:String,imageId:String,albumId:String)
case class Send_getAllAlbumsOfUser(userCount:Int)

case class Send_createPage(userCount: String , dob:String, gender:String, phoneNumber:String)
case class Send_getAllFriendsOfUser(userCount:Int)
case class Send_likePost(authorId: String, postId: String, actionUserId: String)
case class Send_SharePost(authorId: String, postId: String, actionUserId: String)
case class Send_GetPostOfUser(authorId: String, actionUserId: String)
//case class send(user: Int)
//case class stopChk(start: Long)

case class TrackHopsWhileLookUpOfEachFile(success:Int)

object FacebookClient 
{
    private val start:Long=System.currentTimeMillis
    def main(args: Array[String])
  {

	  val system = ActorSystem("ClientSystem")
	  //println("How many Users?")
	  var numOfUsers :Int = 11
    if(args.length==1){
      numOfUsers = args(0).toInt
      println("the number of facebook users for this system is : " + numOfUsers)
    }
	  val client_actor =system.actorOf(Props(new FacebookAPISimulator(system,numOfUsers)),name="FacebookAPISimulator")
	 // val receiver =system.actorOf(Props(new clientReceiver()),name="ClientReceiver")
	  client_actor ! Start(system)
    //system.shutdown()
  }
}

//simulatorn, use statistics , creation and posting , reading according to our studies, with n number of users
class FacebookAPISimulator(system : ActorSystem, userCount : Int) extends Actor 
{
  var clientBuffer= new ArrayBuffer[ActorRef]() 
  var startTime: Long = 0
  private var trackingCounter : Int = 0
	def receive = 
  	{       
  	 case Start(system) => 
  		{
        var pw13 = new FileWriter("client_log.txt",true)
        pw13.write("Hello, welcome to client!! \n") 
        pw13.close()

        startTime = System.currentTimeMillis()
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
          if(i%3==0){
            gender = "M"
          }
          else{
            gender = "F"
            } 
          val areaCode = 300 + rnd.nextInt(700)
          val firstPart = 300 + rnd.nextInt(700)
          val secondPart = 1000 + rnd.nextInt(9000)
          val phoneNumber : String = "("+areaCode.toString+")"+" "+firstPart.toString+"-"+secondPart.toString
  			  if(i%10 == 0){
            client_driver ! Send_createPage(i.toString,dob,gender,phoneNumber)
          }
          else{
            client_driver ! Send_createUser(i.toString,dob,gender,phoneNumber)
          }
  			}


        //client_driver ! Send_getUser(2)

        //client_driver ! Send_getAllUsers(3)
  

        
        

        client_driver ! Send_updateFriendListOfFbUser("1","3","connect")
        client_driver ! Send_updateFriendListOfFbUser("1","4","connect")
       // client_driver ! Send_getAllFriendsOfUser(1)


        //client_driver ! Send_updateFriendListOfFbUser(1,4,"delete")
        //client_driver ! Send_getAllFriendsOfUser(1)
          client_driver ! Send_getAllFriendsOfUser(1)
        client_driver ! Send_getAllFriendsOfUser(3)  

        //post creation apis - do not delete them
        
         client_driver ! Send_createPost("3","First post of the User","1")
         client_driver ! Send_createPost("3","second post of the User","2")

         //client_driver ! Send_getAllFriendsOfUser(1)
         //client_driver ! Send_getAllFriendsOfUser(3)

            //post creation apis - do not delete them
        //client_driver ! Send_createPost("3","First post of the User","1")
        //client_driver ! Send_createPost("3","second post of the User","2")

        //client_driver ! Send_createPost("2","first post of the thh User","3")
        //client_driver ! Send_createPost("2","second post of the thh User","4")

        //client_driver ! Send_getAllFriendsOfUser(1)
        //client_driver ! Send_likePost("3","1","1")

        //client_driver ! Send_getAllFriendsOfUser(1)
        client_driver ! Send_likePost("3","1","1")

        client_driver ! Send_getAllPosts(3)

        client_driver ! Send_GetPostOfUser("3","1")

        //client_driver ! Send_getAllPosts(3)
        //Image creation apis

        client_driver ! Send_createAlbum("1","photo","imageId1","albumId1")
        //client_driver ! Send_createAlbum("1","photo","imageId2","albumId1")
        //client_driver ! Send_createAlbum("1","photo","imageId3","albumId2")

        client_driver ! Send_SharePost("3","1","1")
        //client_driver ! Send_getAllPosts(3)

        
        client_driver ! Send_getAllAlbumsOfUser(1)

        //context.system.shutdown()

        //println("Awesome! Total time taken for the all the search is : " + (System.currentTimeMillis() - startTime) + " milliseconds")  
        }

        case TrackHopsWhileLookUpOfEachFile(success) => {  
          trackingCounter = trackingCounter + 1
          println("trackingCounter is" + trackingCounter)
          println(userCount)
          if( success == 1 && trackingCounter >= userCount + 8){
            println("number of apis processed : " + trackingCounter) 
            println("Awesome!!!!! Total time taken for the all the apis is : " + (System.currentTimeMillis() - startTime) + " milliseconds")  
            context.system.shutdown()
          }
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
  var master: ActorRef = null

	def receive = 
  	{
  		case Send_createUser(userCount,dob,gender,phoneNumber) =>
      {
          //println("bpc1....")
          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/createUser",FormData(Seq("field1"->userCount, "field2"->dob, "field3"->gender, "field4"->phoneNumber))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }     
      }

      case Send_createPage(userCount,dob,gender,phoneNumber) =>
      {
          //println("inside Send_createPage")
          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/createPage",FormData(Seq("field1"->userCount, "field2"->dob, "field3"->gender, "field4"->phoneNumber))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
          }
      }

      case Send_createPost(userCount,content,postId) =>
      {
          //println("bpc6....")
          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/createPost",FormData(Seq("field1"->userCount, "field2"->content,"field3"->postId))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }


      case Send_updateFriendListOfFbUser(userName,friendUserName,action) =>
      {

          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/updateFriendListOfFbUser",FormData(Seq("field1"->userName, "field2"->friendUserName,"field3"->action))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_getUser(userCount) =>
      {
           master = sender           
           val result =  pipeline2(Get("http://localhost:8080/facebook/getProfileInfoOfUser/"+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_getAllUsers(userCount) =>
      {
           master = sender
           
           val result =  pipeline2(Get("http://localhost:8080/facebook/getProfileOfAllFacebookUsers/"+userCount))

           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_getAllPosts(userCount) =>
      {

           master = sender
          
           val result =  pipeline5(Get("http://localhost:8080/facebook/getPostsOfAllFacebookUsers/"+userCount))
           result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_getAllFriendsOfUser(userCount) =>
      {
        master = sender
        val result = pipeline1(Get("http://localhost:8080/facebook/getAllFriendsOfUser/"+userCount))
        result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_likePost(authorId, postId, actionUserId) =>
      {
        master = sender
        val result = pipeline1(Post("http://localhost:8080/facebook/likePost",FormData(Seq("field1"->authorId, "field2"->postId, "field3"->actionUserId))))
        result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_SharePost(authorId, postId, actionUserId) =>
      {
        master = sender
        val result = pipeline1(Post("http://localhost:8080/facebook/sharePost",FormData(Seq("field1"->authorId, "field2"->postId, "field3"->actionUserId))))
        result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_createAlbum(userCount,imageContent,imageId,albumId) =>
      {
          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/createAlbum",FormData(Seq("field1"->userCount, "field2"->imageContent,"field3"->imageId,"field4"->albumId))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_getAllAlbumsOfUser(userCount) =>
      {
        master = sender
        val result = pipeline1(Get("http://localhost:8080/facebook/getAllAlbumsOfUser/"+userCount))
        result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
        
      }
      case Send_GetPostOfUser(authorId,actionUserId)=>
      {  
        master = sender
        val result = pipeline1(Get("http://localhost:8080/facebook/getPostOfUser",FormData(Seq("field1"->authorId, "field2"->actionUserId))))
        result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
           master ! TrackHopsWhileLookUpOfEachFile(1)           
        }
        
      }

    }

  def writeToLog(content :String){ 
        var pw = new FileWriter("client_log.txt",true)
        pw.write(content)
        pw.close()
  }  

}
//}
	








