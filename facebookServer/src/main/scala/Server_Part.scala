package FacebookAPI

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.routing.RoundRobinRouter
import scala.collection.mutable._
import spray.routing.SimpleRoutingApp
import scala.util.Random
import java.io._
import java.util.Date
import java.util.{Date, Locale}

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

import java.io.{File,FileInputStream,FileOutputStream} // For album creation

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

//posts
case class Post(author: String,content: String,likeCount: Int,shareCount: Int) extends Serializable
case class PostMapForTheUser(postMap:HashMap[String,Post])
case class CreatePost(content:String,postId:String)
case class PostMapOfAll(postMapOfAll:HashMap[String,HashMap[String,Post]])
case class GetPostMapOfAllUsers(start:Int,limit:Int)
case class PostMapForAll(userName:String, postMapForTheUser:HashMap[String,Post])
case class LikePost(postId:String, actionUserId:Int)


//albums
case class CreateAlbum(imageContent:String,imageId:String,albumId:String)
case class ImagePost(author: String,imageContent: String) extends Serializable
case class ImageMapAsAlbumForTheUser(albumName:String, imageMapForTheUser:HashMap[String,ImagePost])

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
    //println("here")

         lazy val createUserForFb = post {
          path("facebook" / "createUser") {
            //println("bp1....")
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

        lazy val getAllProfileInfoOfUserOnFb = get {
        respondWithMediaType(MediaTypes.`application/json`)
              path("facebook" / "getProfileOfAllFacebookUsers"){
                parameters("start".as[Int]) { (start) =>
                  //println("here1")                  
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
            //println("bp6....")
                entity(as[FormData]) { fields =>
                    println("In the post Creation spray server")
                      val authorUserName = fields.fields(0)._2
                      val postContent = fields.fields(1)._2
                      val postId = fields.fields(2)._2
                      //val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+authorUserName) 
                      val facebookUser_actor = system.actorSelection("akka://facebookAPI/user/"+"facebookUser"+authorUserName)
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

            lazy val likePostOfUser = post {
              path("facebook"/"likePost"){
                entity(as[FormData]) { fields =>
                  println("inside likePostOfUser")
                  val authorId = fields.fields(0)._2
                  val postId = fields.fields(1)._2
                  val actionUserId = fields.fields(2)._2
                  //val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+authorUserName) 
                  val facebookUser_actor = system.actorSelection("akka://facebookAPI/user/"+"facebookUser"+authorId)
                  facebookUser_actor ! LikePost(postId,actionUserId.toInt)
                  complete("Done")
                }
              }
            }

         lazy val addImageToAnAlbum = post {
          path("facebook" / "createAlbum") {
            println("bp6....")
                entity(as[FormData]) { fields =>
                    println("In the post Creation spray server")
                      val authorUserName = fields.fields(0)._2
                      val imageContent = fields.fields(1)._2
                      val imageId = fields.fields(2)._2
                      var albumId = fields.fields(3)._2
                      //val facebookUser_actor = system.actorOf(Props(new FacebookUser(cache_actor)),name="facebookUser"+authorUserName) 
                      val facebookUser_actor = system.actorSelection("akka://facebookAPI/user/"+"facebookUser"++authorUserName)
                      facebookUser_actor ! CreateAlbum(imageContent,imageId,albumId)
                      complete("Done")
            }
          }
        }



  	     startServer(interface = "localhost", port = 8080) {
          createUserForFb ~
          updateFriendListOfTheUser ~
          profileInfoOfUserOnFb ~
          //profileInfoOfUsers ~
          getAllProfileInfoOfUserOnFb ~
          createPost ~
          getAllPostsOfUserOnFb ~
          likePostOfUser ~
          addImageToAnAlbum

         }
       }
  }       
	  
  class CacheMaster extends Actor
  {
    val profileMapForAllUsers = new scala.collection.mutable.HashMap[String,Profile]()
    val profileList = new ArrayBuffer[Profile]()
    var userFriendMap = new scala.collection.mutable.HashMap[String,List[String]]()
    //var postMapForAllUsers = new scala.collection.mutable.HashMap[String,PostMapForTheUser]()
    var postMapForAllUsers = new scala.collection.mutable.HashMap[String,HashMap[String, Post]]()
    
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
        //println("GetProfileMap inside CacheMaster")
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
      //println("Here too")  
      sender ! ProfileMapForAll(profileMapForAllUsers)
      }  

      case GetPostMapOfAllUsers(start,limit)=>
      {
      println("Here too")  
      sender ! PostMapOfAll(postMapForAllUsers)
      }  

      case AddToFriendListMapOfCache(userName, friendList) => {
          userFriendMap += (userName -> friendList)
          for ((k,v) <- userFriendMap) {
            println("key:"+k+"\tvalue:"+v)
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
    //var likeCount : Int = 0
    //var shareCount : Int = 0
    //var contentOfPost : String =""

    var listOfPosts = List[Post]()
    var postMapForTheUser = new scala.collection.mutable.HashMap[String, Post]()

    //albums
    //var listOfImages = List[ImagePost]()
    //var imageMapForTheAlbum = new scala.collection.mutable.HashMap[String, ImagePost]()

    var imageMapAsAlbumForTheUser = new scala.collection.mutable.HashMap[String,HashMap[String, ImagePost]]()

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
                 friendList = friendList ::: friendList1
                 friendCount = friendList.length
                 putFriendList(userName,friendList)
          }

        case CreatePost(content,postId)=>
          {    
          println("In post Creation")
          println("Username is" + userName);
          println("content is" + content);
          //contentOfPost = content
          //Date datePostCreated = new Date();
          //datePostCreated = Calendar.getInstance().getTime();
          val postObj = Post(userName,content,0,0)
          putPostToMapAndCache(userName,postObj,postId)       
          }


          case LikePost(postId , actionUserId) =>
          {
            println("In case LikePost")
            val actionUserName : String = "facebookUser"+actionUserId
            if (friendList.contains(actionUserName)){
              if(postMapForTheUser.contains(postId)){
                //likeCount = likeCount + 1
                //println("Like Count :"+likeCount)
                //val postObj = Post(userName,contentOfPost,likeCount,shareCount)
                var postObject = postMapForTheUser.get(postId) match{
          case Some(postObject) => postObject
          case None => Post("Error","Error",0,0)
          }     
                var newPostObj = Post(postObject.author,postObject.content,postObject.likeCount+1,postObject.shareCount)
                //postObject.likeCount = postObject.likeCount + 1
                putPostToMapAndCache(userName,newPostObj,postId)
              }
              else{
                println("This post does not belong to author")
              }
            }
            else{
              println("The user is not present in the friend list of the author. Sorry !!, the user cant like the post of the author.")
            }
          }

        case CreateAlbum(imageContent,imageId,albumId)=>
          {    
          println("In albums Creation")
          println("Username is" + userName);
          println("content is" + imageContent);
          val imageObj = ImagePost(userName,imageContent)
          putImageToMapAndCache(userName,imageObj,imageId,albumId)       
          }
      }
     
      def putProfile(userName :String,profileObj:Profile){
        profileMap += (userName -> profileObj)
        cache_actor ! ProfileMap(userName, profileObj)
      }

      def putPostToMapAndCache(userName :String,postObj:Post,postId:String){
        println("putPostToMapAndCache in:")
        //listOfPosts = listOfPosts ::: List(postObj)
        postMapForTheUser += (postId -> postObj)
        for ((k,v) <- postMapForTheUser) {
            println("key:"+k+"\tvalue:"+v)
          //println(v mkString "\n")
        }
        cache_actor ! PostMapForAll(userName, postMapForTheUser)
      }

      def putImageToMapAndCache(userName:String,imageObj:ImagePost,imageId:String,albumId:String){
        println("putImageToMapAndLocalDisk in:")
        transferImagesBetweenClientAndServer(userName,imageObj.imageContent,albumId,imageId)
        imageMapAsAlbumForTheUser += (albumId -> HashMap(imageId -> imageObj))
        for ((k,v) <- imageMapAsAlbumForTheUser) {
            println("key:"+k+"\tvalue:"+v)
          //println(v mkString "\n")
        }
        //cache_actor ! PostMapForAll(userName, postMapForTheUser)
      }


      def transferImagesBetweenClientAndServer(userName:String,destName:String,albumId:String,imageId:String){
            var dir = new File("images/"+userName+"-"+albumId);
            // attempt to create the directory here
            var successful:Boolean  = dir.mkdir();
            val src = new File("common/" + destName + ".jpg")
            val dest = new File("images/"+userName+"-"+albumId+"/"+ destName + "-" +imageId +".jpg")
            if(successful){
            new FileOutputStream(dest) getChannel() transferFrom(
            new FileInputStream(src) getChannel, 0, Long.MaxValue )
            }


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
  def toJson(post:Post) : String = writePretty(post)
  def toJson(postMapOfAll:PostMapOfAll) : String = writePretty(postMapOfAll)
}