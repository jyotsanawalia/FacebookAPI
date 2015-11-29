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

case class Profile(userName: String,dob: String, gender:String, phoneNumber:String, emailId:String, image : String, isPage : Int) extends Serializable
case class ProfileList(profileList : ArrayBuffer[Profile])
case class ProfileInfo(userName:String,profileObject:Profile) extends Serializable
case class SetProfileInfoOfUser(userCount: Int,dob:String,gender:String, phoneNumber:String)
case class SetProfileInfoOfPage(userCount: Int,dob:String,gender:String, phoneNumber:String)
case class GetProfileInfoOfUser(userName:String)

case class ProfileMap(userName: String, profileObject : Profile)
case object GetProfileMap

//added now
case class ProfileMapForAll(profileMap:HashMap[String,Profile])
case class GetProfileMapOfAllUsers(start:Int,limit:Int)


//friendlist
case class UpdateFriendListOfUser(friendList : List[String])
case class AddToFriendListMapOfCache(userName:String, friendList:List[String])
case class FriendListMap(friendlist:HashMap[String,List[String]])
case class GetFriendListOfUser(userName:String)

//page owner list
case class AddToPageOwnerListMapOfCache(userName:String, pageOwnerList:List[String])

//posts
case class Post(author: String,content: String) extends Serializable
case class PostMapForTheUser(postMap:HashMap[String,Post])
case class CreatePost(content:String,postId:String)
case class PostMapOfAll(postMapOfAll:HashMap[String,HashMap[String,Post]])
case class GetPostMapOfAllUsers(start:Int,limit:Int)
case class PostMapForAll(userName:String, postMapForTheUser:HashMap[String,Post])

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
                    //println("Fields = " + fields)
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

        lazy val createPageForFb = post {
          path("facebook" / "createPage") {
            //println("bp7....")
                entity(as[FormData]) { fields =>
                    //println("Fields = " + fields)
                    val userId = fields.fields(0)._2
                    val dob= fields.fields(1)._2
                    val gender = fields.fields(2)._2
                    val phoneNumber = fields.fields(3)._2
                    val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+userId) 
                    facebookUser_actor!SetProfileInfoOfPage(userId.toInt,dob,gender,phoneNumber)
                    complete("Done")
            }
          }
        }

        lazy val updateFriendListOfTheUser = post {
          path("facebook" / "updateFriendListOfFbUser") {
            //println("bp1....")
            parameters("userName".as[String],"friendUserName".as[String],"action".as[String]) { (userName,friendUserName,action) =>
            val facebookUser_actor = system.actorSelection("akka://facebookAPI/user/"+userName)
            val facebookFriend_actor = system.actorSelection("akka://facebookAPI/user/"+friendUserName)
            var friendList = List("facebookUser1", "facebookUser2", "facebookUser3", "facebookUser4", "facebookUser5")
            var friendListOfUser : List[String] = List[String]()
            var friendListOfFriend : List[String] = List[String]()
             
            if(action=="update"){
                 friendListOfUser = friendList.filter(_ != userName)
                 friendListOfFriend = friendList.filter(_ != friendUserName)
            }else if(action=="add"){
                friendListOfUser = friendList.filter(_ != userName)
                friendListOfFriend = friendList.filter(_ != friendUserName)// add here
            }else if(action=="delete"){
                friendListOfUser = friendList.filter(_ != userName)
                friendListOfFriend = friendList.filter(_ != friendUserName)// subtract here
            }else if(action == "connect"){
                // friendListOfUser = friendList.filter(_ == friendUserName)
                // friendListOfFriend = friendList.filter(_ == userName)
                friendListOfUser = List(friendUserName)
                friendListOfFriend = List(userName)
            }
            
            facebookUser_actor ! UpdateFriendListOfUser(friendListOfUser)
            facebookFriend_actor ! UpdateFriendListOfUser(friendListOfFriend)
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

              lazy val getFriendListOfUser = get {
                respondWithMediaType(MediaTypes.`application/json`)
                path("facebook"/"getAllFriendsOfUser"/Segment){  userCount =>
                  val userName = "facebookUser"+userCount
                  //val actor = system.actorSelection("akka://facebookAPI/user/"+userCount)
                  val future = cache_actor ? GetFriendListOfUser(userName)
                  val friendList = Await.result(future, timeout.duration).asInstanceOf[FriendListMap]
                  complete{
                    JsonUtil.toJson(friendList)
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

        // lazy val createPost = post {
        //   path("facebook" / "createPost" ) { 
        //       entity(as[FormData]) { fields =>
        //               println("In the post Creation spray server")
        //               val authorUserName = fields.fields(0)._2
        //               val postContent = fields.fields(1)._2
        //               val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+authorUserName) 
        //               facebookUser_actor ! CreatePost(postContent)      
        //       }
        //       complete("Done")
        //     }
        //   }

          lazy val createPost = post {
          path("facebook" / "createPost") {
            println("bp6....")
                entity(as[FormData]) { fields =>
                    println("In the post Creation spray server")
                      val authorUserName = fields.fields(0)._2
                      val postContent = fields.fields(1)._2
                      val postId = fields.fields(2)._2
                      //val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+authorUserName) 
                      val facebookUser_actor = system.actorSelection("akka://facebookAPI/user/"+"facebookUser"++authorUserName)
                      facebookUser_actor ! CreatePost(postContent,postId)
                      complete("Done")
            }
          }
        }


          lazy val getAllPostsOfUserOnFb = get {
              respondWithMediaType(MediaTypes.`application/json`)
              path("facebook" / "getPostsOfAllFacebookUsers"){
                parameters("start".as[Int]) { (start) =>
                  println("in the spray server of get all posts")                  
                   val future = cache_actor ? GetPostMapOfAllUsers(start,10)
                   val userPostsHashMap = Await.result(future, timeout.duration).asInstanceOf[PostMapOfAll]
                   complete{ 
                    //userProfileHashMap
                   JsonUtil.toJson(userPostsHashMap)

                   }
                }
                
              }
            }

        

  	     startServer(interface = "localhost", port = 8080) {
          createUserForFb ~
          createPageForFb ~
          updateFriendListOfTheUser ~
          profileInfoOfUserOnFb ~
          //profileInfoOfUsers ~
          getAllProfileInfoOfUserOnFb ~
          getFriendListOfUser ~
          createPost ~
          getAllPostsOfUserOnFb

         }
       }
  }       
	  
  class CacheMaster extends Actor
  {
    val profileMapForAllUsers = new scala.collection.mutable.HashMap[String,Profile]()
    val profileList = new ArrayBuffer[Profile]()
    var userFriendMap = new scala.collection.mutable.HashMap[String,List[String]]()
    var pageOwnerMap = new scala.collection.mutable.HashMap[String,List[String]]()
    //var postMapForAllUsers = new scala.collection.mutable.HashMap[String,PostMapForTheUser]()
    var postMapForAllUsers = new scala.collection.mutable.HashMap[String,HashMap[String, Post]]()
    var friendListMapOfUser = new scala.collection.mutable.HashMap[String,List[String]]()
    var emptyList : List[String] = List("","","")

    def receive =
    {
      case ProfileMap(userName, profileObject)=>
      {
        profileMapForAllUsers += (userName -> profileObject)
      }

      case PostMapForAll(userName, postMapForTheUser)=>
      {
        postMapForAllUsers += (userName -> postMapForTheUser)
        //postMapForAllUsers.update(userName, postMapForTheUser)
        println("In PostMapForAll---all function : ")
        for ((k,v) <- postMapForAllUsers) {
            println("key:"+k+"\tvalue:"+v)
          //println(v mkString "\n")
        }
        println("end")
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
             case None => Profile("Error","Error","Error","Error","Error","Error", 0)
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

      case GetPostMapOfAllUsers(start,limit)=>
      {
      println("Here too")  
      sender ! PostMapOfAll(postMapForAllUsers)
      }  

      case AddToFriendListMapOfCache(userName, friendList) => {
          userFriendMap += (userName -> friendList)
          //println("friend list of user : "+userName)
          //val friendListObj = FriendListMap(userFriendMap)
          //for ((k,v) <- userFriendMap) {
            //println("friend list of username:"+userName+"\tkey:"+k+"\tvalue:"+v)
          //}
          //println("next")
      }

      case AddToPageOwnerListMapOfCache(userName, pageOwnerList) => {
        pageOwnerMap += (userName -> pageOwnerList)
        //println("page owner list for page : "+userName)
        //for((k,v) <- pageOwnerMap){
          //println("key:"+k+"\tvalue:"+v)
        //}
      }

      case GetFriendListOfUser(userName) => {
        val friendList : List[String]= userFriendMap.get(userName) match{
          case Some(friendList) => friendList
          case None => emptyList
          }
          friendListMapOfUser += (userName -> friendList)
        sender ! FriendListMap(friendListMapOfUser)
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

    var listOfPosts = List[Post]()
    var postMapForTheUser = new scala.collection.mutable.HashMap[String, Post]()
    def receive = 
      {    
        case SetProfileInfoOfUser(userCount,dob,gender,phoneNumber)=>
          {    
            //println("bp2....")
            userName = "facebookUser"+userCount;
            emailId = userName+"@gmail.com"
            //println("Username is : " + userName);
            //println("Date Of Birth of user is : " + dob);
            //println("Gender is : "+gender)
            //println("Phone Number is : "+phoneNumber)
            //println("isPage : "+isPage)
            val profileObj = Profile(userName,dob,gender,phoneNumber,emailId,image,isPage)
            putProfile(userName,profileObj)       
          }

        case SetProfileInfoOfPage(userCount,dob,gender,phoneNumber)=>
          {
            //println("bp7....")
            isPage = 1
            userName = "facebookPage"+userCount;
            emailId = userName+"@gmail.com"
            //println("Page name is : " + userName);
            //println("Date Of Birth of user is : " + dob);
            //println("Gender is : "+gender)
            //println("Phone Number is : "+phoneNumber)
            //println("isPage : "+isPage)
            var pageOwnerList = List("facebookUser1", "facebookUser2", "facebookUser3", "facebookUser4", "facebookUser5")
            val profileObj = Profile(userName,dob,gender,phoneNumber,emailId,image,isPage)
            putProfile(userName,profileObj)
            putPageOwnerList(userName,pageOwnerList)     
          }

      case GetProfileInfoOfUser(userName)=>
        { 
          val profileObject = profileMap.get(userName) match{
          case Some(profileObject) => profileObject
          case None => Profile("Error","Error","Error","Error","Error","Error",0)
          }
          sender ! profileObject
        } 

        case UpdateFriendListOfUser(friendList1)=>
          {    
                friendList = friendList ::: friendList1
                friendCount = friendList.length
                //sprintln("userName : "+userName)
                //val friendListObject = FriendList(userName, friendlist)
                putFriendList(userName,friendList)
          }

        case CreatePost(content,postId)=>
          {    
          println("In post Creation")
          println("Username is" + userName);
          println("content is" + content);
          val postObj = Post(userName,content)
          putPostToMapAndCache(userName,postObj,postId)       
          }

      }

      def putProfile(userName :String,profileObj:Profile){
        profileMap += (userName -> profileObj)
        cache_actor ! ProfileMap(userName, profileObj)
      }

      def putPostToMapAndCache(userName :String,postObj:Post,postId:String){
        println("putPostToMapAndCache in:")
        listOfPosts = listOfPosts ::: List(postObj)
        postMapForTheUser += (postId -> postObj)
        for ((k,v) <- postMapForTheUser) {
            println("key:"+k+"\tvalue:"+v)
          //println(v mkString "\n")
        }
        cache_actor ! PostMapForAll(userName, postMapForTheUser)
      }

      def putFriendList(userName :String,friendList:List[String]){ 
        cache_actor ! AddToFriendListMapOfCache(userName, friendList)
      }

      def putPageOwnerList(userName:String,pageOwnerList:List[String]){
        cache_actor ! AddToPageOwnerListMapOfCache(userName, pageOwnerList)
      }
    
  }

object JsonUtil{
  
  implicit val formats = native.Serialization.formats(ShortTypeHints(List(classOf[ProfileMap])))
  // implicit val formats = native.Serialization.formats(ShortTypeHints(List(classOf[Profile])))
  def toJson(profile:Profile) : String = writePretty(profile)
  def toJson(profileList:ProfileList) : String = writePretty(profileList)
  def toJson(profileMap:ProfileMapForAll) : String = writePretty(profileMap)
  def toJson(post:Post) : String = writePretty(post)
  def toJson(postMapOfAll:PostMapOfAll) : String = writePretty(postMapOfAll)
  def toJson(friendlist:FriendListMap) : String = writePretty(friendlist)
}