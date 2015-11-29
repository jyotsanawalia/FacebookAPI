package FacebookAPI

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.routing.RoundRobinRouter
import scala.collection.mutable._
import spray.routing.SimpleRoutingApp
import scala.util.Random
import java.io._

import scala.util.{Success, Failure}

//spray stuff
import akka.routing.ConsistentHashingRouter
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.ArrayList
import scala.collection.mutable.MutableList
import java.security.MessageDigest
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
//spray client stuff
import akka.actor._
import spray.routing.SimpleRoutingApp

import org.json4s._
import org.json4s.native.Serialization.{read, write,writePretty}
import spray.http._
import spray.routing.{Route, RequestContext}
import spray.routing.directives._

case class Profile(userName: String,dob: String, gender:String, phoneNumber:String, emailId:String, image : String) extends Serializable
case class ProfileList(profileList : ArrayBuffer[Profile])
case class ProfileInfo(userName:String,profileObject:Profile) extends Serializable
case class SetProfileInfoOfUser(userCount: Int,dob:String,gender:String, phoneNumber:String)
case class GetProfileInfoOfUser(userName:String)

case class ProfileMap(userName: String, profileObject : Profile)
case object GetProfileMap

//added now
case class ProfileMapForAll(profileMap:HashMap[String,Profile])
case class GetProfileMapOfAllUsers(start:Int,limit:Int)


//friendlist
case class UpdateFriendListOfUser(friendList : List[String])
case class AddToFriendListMapOfCache(userName:String, friendList:List[String])

object FacebookServer extends App with SimpleRoutingApp
{ 

  override def main(args: Array[String]) 
  {
    //import system.dispatcher
	  implicit val system = ActorSystem("facebookAPI")
	  val actorCount: Int = Runtime.getRuntime().availableProcessors()*100
    implicit val timeout =  akka.util.Timeout(50000)
    //var profileList = new java.util.ArrayList[Profile]()

	  println("Facebook Server Started....")
    val cache_actor = system.actorOf(Props[CacheMaster], name="cache_actor")
    println("here")

         lazy val createUserForFb = post {
          path("facebook" / "createUser") {
            println("bp1....")
                entity(as[FormData]) { fields =>
                    println("Fields = " + fields)
                    val userId = fields.fields(0)._2
                    val dob= fields.fields(1)._2
                    val gender = fields.fields(2)._2
                    val phoneNumber = fields.fields(3)._2
                    val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+userId) 
                    facebookUser_actor!SetProfileInfoOfUser(userId.toInt,dob,gender,phoneNumber)
                    complete("Done")
            }
          }
        }

        lazy val updateFriendListOfTheUser = post {
          path("facebook" / "updateFriendListOfFbUser") {
            println("bp1....")
            parameters("userName".as[String],"friendUserName".as[String],"action".as[String]) { (userName,friendUserName,action) =>
            val facebookUser_actor = system.actorSelection("akka://facebookAPI/user/"+userName)
            var friendList = List("facebookUser1", "facebookUser2", "facebookUser3", "facebookUser4", "facebookUser5")
            if(action=="update"){
                 friendList = friendList.filter(_ != userName)
            }else if(action=="add"){
                friendList = friendList.filter(_ == userName)// add here
            }else if(action=="delete"){
                friendList = friendList.filter(_ != userName)// subtract here
            }
            
            facebookUser_actor ! UpdateFriendListOfUser(friendList)
              complete {
                "updated for user="+userName
              }
            }
          }
        }

        lazy val profileInfoOfUserOnFb = get {
        respondWithMediaType(MediaTypes.`application/json`)
              path("facebook" / "getProfileInfoOfUser"/Segment){ userCount =>
                val userName = "facebookUser"+userCount
                //parameters("userName".as[String]) { (userName) =>
                    val actor = system.actorSelection("akka://facebookAPI/user/"+userName)
                   // implicit val timeout =  Timeout(2 seconds)
                   val future = actor ? GetProfileInfoOfUser(userName)
                   val userProfile = Await.result(future, timeout.duration).asInstanceOf[Profile]
                   complete{ 
                   JsonUtil.toJson(userProfile)//change it
                   }
                }
                
              }

        lazy val getAllProfileInfoOfUserOnFb = get {
        respondWithMediaType(MediaTypes.`application/json`)
              path("facebook" / "getProfileOfAllFacebookUsers"){
                parameters("start".as[Int]) { (start) =>
                  println("here1")                  
                   val future = cache_actor ? GetProfileMapOfAllUsers(start,10)
                   val userProfileHashMap = Await.result(future, timeout.duration).asInstanceOf[ProfileMapForAll]
                   complete{ 
                    //userProfileHashMap
                   JsonUtil.toJson(userProfileHashMap)

                   }
                }
                
              }
            }
        

  	   startServer(interface = "localhost", port = 8080) {
        
        createUserForFb ~
        updateFriendListOfTheUser ~
        profileInfoOfUserOnFb ~
        getAllProfileInfoOfUserOnFb
        
         }
       }
  }       
	  
  class CacheMaster extends Actor
  {
    val profileMapForAllUsers = new scala.collection.mutable.HashMap[String,Profile]()
    val profileList = new ArrayBuffer[Profile]()
    var userFriendMap = new scala.collection.mutable.HashMap[String,List[String]]()

    def receive =
    {
      case ProfileMap(userName, profileObject)=>
      {
        profileMapForAllUsers += (userName -> profileObject)
      }

      case GetProfileMap=>
      {
        println("GetProfileMap inside CacheMaster")
         for(i<-0 until profileMapForAllUsers.size){
          var userName : String = "facebookUser"+i
           var profileObject = profileMapForAllUsers.get(userName) match{
             case Some(profileObject) =>
             {
               println("profileObject: "+profileObject)
               profileList += profileObject
             }
             case None => Profile("Error","Error","Error","Error","Error","Error")
             }
      }
      val profileListObject = ProfileList(profileList)
      sender ! profileListObject
      }  

      case GetProfileMapOfAllUsers(start,limit)=>
      {
      println("Here too")  
      sender ! ProfileMapForAll(profileMapForAllUsers)
      }   

      case AddToFriendListMapOfCache(userName, friendList) => {
          userFriendMap += (userName -> friendList)
          for ((k,v) <- userFriendMap) {
            println("key:"+k+"\tvalue:"+v)
          println(v mkString "\n")
        }
      }

    }
  }
//}

  //this class actually denotes the user actor of facebook
  class FacebookUser(cache_actor:ActorRef) extends Actor 
  {
    val writer = new FileWriter("Server_Output.txt",true )
    var profileMap = new scala.collection.mutable.HashMap[String, Profile]()
    var userName:String = ""
    var emailId : String = ""
    var isPage : Int = 0
    var image : String = "C:-Users-jyotsana-Desktop-FacebookAPI-facebookHelper-photo.jpg"
    var friendList : List[String] = List[String]()
    var friendCount : Int = 0
    def receive = 
      {    
        case SetProfileInfoOfUser(userCount,dob,gender,phoneNumber)=>
          {    
            println("bp2....")
            userName = "facebookUser"+userCount;
            emailId = userName+"@gmail.com"
            println("Username is : " + userName);
            println("Date Of Birth of user is : " + dob);
            println("Gender is : "+gender)
            println("Phone Number is : "+phoneNumber)
            val profileObj = Profile(userName,dob,gender,phoneNumber,emailId,image)
            putProfile(userName,profileObj)       
          }

          // case SetProfileInfoOfPage()
          // {

          // }

      case GetProfileInfoOfUser(userName)=>
        { 
          val profileObject = profileMap.get(userName) match{
          case Some(profileObject) => profileObject
          case None => Profile("Error","Error","Error","Error","Error","Error")
          }
          sender ! profileObject
        } 

        case UpdateFriendListOfUser(friendList1)=>
          {    
                 friendList = friendList1
                 friendCount = friendList1.length
                 putFriendList(userName,friendList)
          }

      }

      


      def putProfile(userName :String,profileObj:Profile){
        profileMap += (userName -> profileObj)
        cache_actor ! ProfileMap(userName, profileObj)
      }

      def putFriendList(userName :String,friendList:List[String]){ 
        cache_actor ! AddToFriendListMapOfCache(userName, friendList)
      }
    
  }

object JsonUtil{
  
  implicit val formats = native.Serialization.formats(ShortTypeHints(List(classOf[ProfileMap])))
  // implicit val formats = native.Serialization.formats(ShortTypeHints(List(classOf[Profile])))
  def toJson(profile:Profile) : String = writePretty(profile)
  def toJson(profileList:ProfileList) : String = writePretty(profileList)
  def toJson(profileMap:ProfileMapForAll) : String = writePretty(profileMap)
}