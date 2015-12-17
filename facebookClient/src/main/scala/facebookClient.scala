package FacebookAPI

import java.io.{File,FileInputStream,FileOutputStream}
import java.io.FileWriter;
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

import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout
import scala.util.Random
//import scala.concurrent.ExecutionContext.Implicits.global


//Security imports
import java.security.KeyPair
import java.security.Key
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import scala.collection.mutable.ArrayBuffer
import akka.actor._
import java.util.Calendar
import java.security.SecureRandom
import javax.crypto.Cipher
import java.security.spec._
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import java.security.KeyFactory
import java.security.Signature

//for image to string
import org.apache.commons.codec.binary.Base64
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;


//For the AES-256 encryption
import java.security.MessageDigest
import java.util
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
//import org.apache.commons.codec.binary.Base64
//import org.apache.commons.io.IOUtils

//For json encode
import org.json4s._
import org.json4s.native.JsonMethods._


case class Start(system : ActorSystem)
case class Send_createUser(userCount: String , dob:String, gender:String, phoneNumber:String)
case class Send_getUser(userCount: Int)
case class Send_getAllUsers(userCount:Int)
case class Send_getAllPosts(userCount:Int)
case class Send_updateFriendListOfFbUser(userName:String,friendUserName:String,action:String)
case class Send_createPost(userCount:String,content:String,postId:String)

case class Send_createAlbum(userCount:String,imageContent:String,imageId:String,albumId:String)
case class Send_getAllAlbumsOfUser(userCount:Int)
case class Send_getPicOfUserByImageId(authorId : String,actionUserId:String ,imageId:String ,albumId:String) //new

case class Send_createPage(userCount: String , dob:String, gender:String, phoneNumber:String)
case class Send_getAllFriendsOfUser(userCount:Int)
case class Send_likePost(authorId: String, postId: String, actionUserId: String)
case class Send_SharePost(authorId: String, postId: String, actionUserId: String)

case class Send_GetPostOfUser(authorId: String, actionUserId: String)
case class Send_GetPostOfUserByPostId(authorId: String, actionUserId: String,postId: String)

case class TrackHopsWhileLookUpOfEachFile(success:Int)

//security cases
//case class Secure_Register(userCount: String, dob:String, gender:String, phoneNumber:String, publicKey:String)
case class Secure_Register(userCount: String, dob:String, gender:String, phoneNumber:String)
case class Secure_Login(usercount:String, action:String)
//case class SecureRandomNumber(userCount : String,aesKey : String)
case class KeysArray(publicKey : PublicKey, privateKey:PrivateKey)
case class Secure_Connect(usercount:String, randomNumberString:String, signatureString:String)
case class GiveAccess(actionUserId : String, publicKey : PublicKey)
case class Send_SecureGetPostOfUserByPostId(authorId: String, actionUserId: String,postId: String)
case class GiveSecureAccessToPost(actionUserId : String, publicKeyOfRequestor : PublicKey, postId : String) 

object FacebookClient 
{
    private val start:Long=System.currentTimeMillis
    def main(args: Array[String])
  {

	  val system = ActorSystem("ClientSystem")
    var numOfUsers :Int = 5
    if(args.length==1){
      numOfUsers = args(0).toInt
      println("the number of facebook users for this system is : " + numOfUsers)
    }
	  val client_actor =system.actorOf(Props(new FacebookAPISimulator(system,numOfUsers)),name="FacebookAPISimulator")
	  client_actor ! Start(system)
  }
}

object Encryption {
  def encrypt(key: String, value: String): String = {
    val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key))
    Base64.encodeBase64String(cipher.doFinal(value.getBytes("UTF-8")))
  }

  def decrypt(key: String, encryptedValue: String): String = {
    val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key))
    new String(cipher.doFinal(Base64.decodeBase64(encryptedValue)))
  }

  def keyToSpec(key: String): SecretKeySpec = {
    var keyBytes: Array[Byte] = (SALT + key).getBytes("UTF-8")
    val sha: MessageDigest = MessageDigest.getInstance("SHA-1")
    keyBytes = sha.digest(keyBytes)
    keyBytes = util.Arrays.copyOf(keyBytes, 16)
    new SecretKeySpec(keyBytes, "AES")
  }

  private val SALT: String =
    "jMhKlOuJnM34G6NHkqo9V010GhLAqOpF0BePojHgh1HgNg8^72k"
}

//simulatorn, use statistics , creation and posting , reading according to our studies, with n number of users
class FacebookAPISimulator(system : ActorSystem, userCount : Int) extends Actor 
{
  val ALGORITHM : String = "RSA"
  var clientBuffer= new ArrayBuffer[ActorRef]() 
  var startTime: Long = 0
  private var trackingCounter : Int = 0
  val keysHashMap = new scala.collection.mutable.HashMap[String,KeysArray]
  val client_driver = new ArrayBuffer[ActorRef]()


    
	def receive = 
  	{       
  	 case Start(system) => 
  		{
        var pw13 = new FileWriter("client_log.txt",true)
        pw13.write("Hello, welcome to client!! \n") 
        pw13.close()

        startTime = System.currentTimeMillis()
        var weightToRead = 0.5
        var weightToLike = 0.03
        var weighToPost = 0.04
        var weightToPostPicture = 0.03

  			//val client_driver = context.actorOf(Props(new FacebookAPIClient(system)),name="FacebookAPIClient") 					
  			var gender :  String = ""
        for(i <-0 until userCount) 
  			{
          client_driver += context.actorOf(Props(new FacebookAPIClient(system)),name="FacebookAPIClient:"+i.toString)
        
        
        //RSA encryption and decryption tested
        // var bytes = RSA.encrypt("secret message",publicKey)
        // println("encryption")
        // println("bytes = "+bytes)

        // var message = RSA.decrypt(bytes,privateKey)
        // println("decryption")
        // println("message = "+message)

        //val keysArrayObj = new KeysArray(publicKey,privateKey) // Storing public and private keys in an KeysArrayObject
        //keysHashMap += (i.toString -> keysArrayObj)

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
  			  //if(i%10 == 0){
            //client_driver ! Send_createPage(i.toString,dob,gender,phoneNumber)
          //}
          //else{
            //client_driver(i) ! Secure_Register(i.toString,dob,gender,phoneNumber,pubKeyStr)
            client_driver(i) ! Secure_Register(i.toString,dob,gender,phoneNumber)
            //client_driver(i) ! SetKeys(publicKey,privateKey,)
            
            //client_driver ! Send_createUser(i.toString,dob,gender,phoneNumber)
          //}
  			}

        //client_driverMain ! Secure_Register("10","fwew","M","12312312")

        for(i <-0 until userCount) 
        {
          client_driver(i) ! Secure_Login(i.toString,"login")
        }

        var j = 0
        //client_driverMain ! Secure_Login("10","login")
        client_driver(1) ! Send_updateFriendListOfFbUser("1","3","connect")
        for(i <- 1 to 2000){
          j = j +1
        }
        client_driver(1) ! Send_updateFriendListOfFbUser("1","4","connect")

        for(i <- 1 to 2000){
          j = j + 1
        }
        client_driver(1) ! Send_getAllFriendsOfUser(1)
         
        for(i <- 1 to 2000){
          j = j + 1
        }
        client_driver(1) ! Send_createPost("1","First post of the User","1")
        for(i <- 1 to 4000){
          j = j+ 1
        }
        //client_driver(1) ! Send_createPost("1","second post of the User","2")

        client_driver(3) ! Send_GetPostOfUserByPostId("1","3","1")
        for(i <- 1 to 2000){
          j = j+ 1
        }

        //client_driver(4) ! Send_GetPostOfUserByPostId("1","4","1")

        client_driver(1) ! Send_createAlbum("1","image2","imageId1","albumId1")
        for(i <- 1 to 2000){
          j = j+ 1
        }
        //client_driver(3) ! Send_getPicOfUserByImageId("1","3","imageId1","albumId1")

        //client_driver ! Send_createAlbum("1","photo","imageId1","albumId1")
        //client_driver ! Send_getAllAlbumsOfUser(1)


        //client_driver ! Send_updateFriendListOfFbUser("1","3","connect")
        //client_driver ! Send_updateFriendListOfFbUser("1","4","connect")
        //val theAesKey = "My very own, very private key here!"
        //var aesEncryptedMessage:String = Encryption.encrypt(theAesKey, "First post of the User")
        //println("aesEncryptedMessage :" + aesEncryptedMessage)

        //client_driver(1) ! Send_createPost("1","First post of the User","1")
        //client_driver(1) ! Send_createPost("1","second post of the User","2")
        //client_driver(2) ! Send_createPost("2","first post of the User","3")
        //client_driver(2) ! Send_createPost("2","second post of the User","4")

        //client_driver(1) ! Send_createAlbum("1","image2","imageId1","albumId1")
        //client_driver(3) ! Send_getPicOfUserByImageId("1","3","imageId1","albumId1")
        //client_driver(1) ! Send_createAlbum("1","photo","imageId2","albumId1")

        //client_driver(3) ! Send_GetPostOfUserByPostId("1","3","1")

        //client_driver(3) ! Send_GetPostOfUserByPostId("1","3","1")
        client_driver(3) ! Send_SecureGetPostOfUserByPostId("1","3","1")
        for(i <- 1 to 2000){
          j = j+ 1
        }

        client_driver(3) ! Send_getPicOfUserByImageId("1","3","imageId1","albumId1")
        for(i <- 1 to 2000){
          j = j+ 1
        }

        //simulate()

}

        case TrackHopsWhileLookUpOfEachFile(success) => {  
          trackingCounter = trackingCounter + 1
          println("trackingCounter is" + trackingCounter)
          var track = 8
          // if((userCount-4)%8 == 0){
          //   track = 53
          // }
          // if(success == 1 && trackingCounter == 20){
          //   context.system.shutdown()
          // }
          
          // if( success == 1 && trackingCounter >= 2*userCount + track){
          //   println("number of apis processed : " + trackingCounter) 
          //   println("Awesome!!!!! Total time taken for the all the apis is : " + (System.currentTimeMillis() - startTime) + " milliseconds")  
          //   context.system.shutdown()
          // }
        }
      }

      def simulate(){

        var numberToScale :Int = 100
        if(userCount > 100){
          numberToScale = 100
        }


        //client_driver(1) ! Send_updateFriendListOfFbUser("1","3","connect")
        //client_driver(1) ! Send_updateFriendListOfFbUser("1","4","connect")

        // var arrayOfUser = new ArrayBuffer[Int]() // 33
        // var i:Int = 4
        // while (i < numberToScale){
        //  for(a <- 1 until 3){
        //       client_driver ! Send_updateFriendListOfFbUser(i.toString,a.toString,"connect")
        //  } 
        //   arrayOfUser += i
        //   i = i+8
        // }


        //Encrypt the post data before creating it!
        //val theAesKey = "My very own, very private key here!"
        //var aesEncryptedMessage:String = Encryption.encrypt(theAesKey, "First post of the User")
        //println("aesEncryptedMessage :" + aesEncryptedMessage)
        //client_driver(3) ! Send_createPost("3",aesEncryptedMessage,"1")

        client_driver(1) ! Send_updateFriendListOfFbUser("1","3","connect")
        client_driver(1) ! Send_updateFriendListOfFbUser("1","4","connect")
        //client_driver(3) ! Send_updateFriendListOfFbUser("3","1","connect")

        //client_driver(3) ! Send_createPost("3","First post of the User","1")
        //client_driver(3) ! Send_createPost("3","second post of the User","2")
        //client_driver(2) ! Send_createPost("2","first post of the User","3")
        //client_driver(2) ! Send_createPost("2","second post of the User","4")

        
        //client_driver(1) ! Send_GetPostOfUser("3","1")

        // i = 1
        // for(user <- arrayOfUser){
        //       client_driver ! Send_createPost(user.toString,"Post is about distributed operating system," + i +" numbered post!","post"+i.toString)
        //       i = i+1
        // }

        client_driver(1) ! Send_createAlbum("1","photo","imageId1","albumId1")
        client_driver(1) ! Send_createAlbum("1","photo","imageId2","albumId1")
        //client_driver(1) ! Send_createAlbum("1","photo","imageId3","albumId2")
       
        //client_driver(3) ! Send_getPicOfUserByImageId("1","3","imageId1","albumId1")

        // client_driver ! Send_likePost("3","1","1")
        // client_driver ! Send_SharePost("3","1","1")

        // Simulation ends , below are all the sample get APIS // refer to client_log.txt
        //client_driver ! Send_getUser(2)
        //client_driver ! Send_getAllUsers(3)
        client_driver(1) ! Send_getAllFriendsOfUser(1)
        client_driver(3) ! Send_getAllFriendsOfUser(3)
        //client_driver ! Send_getAllPosts(3)
        //client_driver(3) ! Send_GetPostOfUser("3","1",theAesKey)
        //client_driver(3) ! Send_GetPostOfUserByPostId("3","1","1",theAesKey)
        //client_driver(1) ! Send_getAllAlbumsOfUser(1)
        //client_driver(1) ! Send_GetPostOfUserByPostId("3","1","1")
        client_driver(3) ! Send_getPicOfUserByImageId("1","3","imageId1","albumId1")

      }
  }

    object RSA
    {
      def encrypt(text : String, publicKey : PublicKey) : Array[Byte] = {
         val cipher : Cipher = Cipher.getInstance("RSA")
         cipher.init(Cipher.ENCRYPT_MODE, publicKey)
         var cipherText : Array[Byte] = cipher.doFinal(text.getBytes())
         cipherText
       }

      def decrypt(byteArray : Array[Byte], privateKey : PrivateKey) : String = {

        println("decryption")

        // get an RSA cipher object and print the provider
        val cipher : Cipher = Cipher.getInstance("RSA")
        println("decryption1")
        // decrypt the text using the private key
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        println("decryption2")
        var dectyptedText : Array[Byte] = cipher.doFinal(byteArray)
        println("decryption3")
        return new String(dectyptedText)

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
  var actorRequestor : ActorRef = null

  implicit val timeout =  akka.util.Timeout(50000)

  var name = self.path.name 
  var userId = name.split(":").last

  //RSA Keys generation 
  val keyGen : KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
  keyGen.initialize(2048)
  val key : KeyPair = keyGen.generateKeyPair()
  val publicKey : PublicKey = key.getPublic()
  val privateKey : PrivateKey = key.getPrivate()
  var publicKeyBytes : Array[Byte] = publicKey.getEncoded()
  val encoder : BASE64Encoder  = new BASE64Encoder()
  val pubKeyStr : String = encoder.encode(publicKeyBytes) // converting public key to string
  var privateKeyBytes : Array[Byte] = privateKey.getEncoded()
  val priKeyStr : String = encoder.encode(privateKeyBytes) // converting private key to string
  var bytesArray = Array[Byte]()

  //
  var publicKeyMap = new scala.collection.mutable.HashMap[String,PublicKey]()

  var friendsPublicKey : PublicKey = null
  //AES key
  val theAesKey = "My AES key here!"+userId

	def receive = 
  	{
  		case Send_createUser(userCount,dob,gender,phoneNumber) =>
      {
          master = sender
          println("publicKey at client which is sent to server : "+publicKey)
          val result = pipeline1(Post("http://localhost:8080/facebook/createUser",FormData(Seq("field1"->userCount, "field2"->dob, "field3"->gender, "field4"->phoneNumber))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }     
      }

      //registering the new user and sending its public key to the server
      case Secure_Register(userCount,dob,gender,phoneNumber) =>
      {
        master = sender
        val result = pipeline1(Post("http://localhost:8080/facebook/secure_registerUser",FormData(Seq("field1"->userCount, "field2"->dob, "field3"->gender, "field4"->phoneNumber, "field5"->pubKeyStr))))
        result.foreach { response =>
          println(s"Request in Secure_Register completed with status ${response.status} and content:\n${response.entity.asString}")
          writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
          master ! TrackHopsWhileLookUpOfEachFile(1) 
        }     
      }

      //login
      case Secure_Login(usercount,action) =>
      {
        master = sender
        val result = pipeline1(Post("http://localhost:8080/facebook/secure_login", FormData(Seq("field1"->usercount, "field2"->action))))
        result.foreach { response =>
          println(s"Request in Secure_Login completed with status ${response.status} and content:\n${response.entity.asString}")
          writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
          var randomNumberString : String = response.entity.asString
          var signatureString = secureRandomNumber(randomNumberString)
          secure_Connect(usercount,randomNumberString,signatureString)
          master ! TrackHopsWhileLookUpOfEachFile(1) 

        }    
      } 

      case Send_createPage(userCount,dob,gender,phoneNumber) =>
      {
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
          var aesEncryptedMessage:String = Encryption.encrypt(theAesKey, content)
          val signedAesEncryptedMessage : Array[Byte] = computeSignature(aesEncryptedMessage)
          val signedAesEncryptedMessageString : String = encoder.encode(signedAesEncryptedMessage)
          //println("aesEncryptedMessage :" + aesEncryptedMessage)

          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/createPost",FormData(Seq("field1"->userCount, "field2"->aesEncryptedMessage,"field3"->postId, "field4"->signedAesEncryptedMessageString))))
          result.foreach { response =>
           println(s"Request completed in Send_createPost with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
           master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }


      case Send_updateFriendListOfFbUser(userName,friendUserName,action) =>
      {

           master = sender
           val result = pipeline1(Post("http://localhost:8080/facebook/updateFriendListOfFbUser",FormData(Seq("field1"->userName, "field2"->friendUserName,"field3"->action))))
           result.foreach { response =>
           println(s"Request completed in Send_updateFriendListOfFbUser with status ${response.status} and content:\n${response.entity.asString}")
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
          val bis = new BufferedInputStream(new FileInputStream("common/" + imageContent +".jpg"))
          val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
          val bytes64 = Base64.encodeBase64(bArray)
          val imageContent1 = new String(bytes64)
          var aesEncryptedImageString:String = Encryption.encrypt(theAesKey, imageContent1)

          var aesEncryptedImage:String = Encryption.encrypt(theAesKey, aesEncryptedImageString)
          val signedAesEncryptedImage : Array[Byte] = computeSignature(aesEncryptedImage)
          val signedAesEncryptedImageString : String = encoder.encode(signedAesEncryptedImage)

          master = sender
          val result = pipeline1(Post("http://localhost:8080/facebook/createAlbum",FormData(Seq("field1"->userCount, "field2"->aesEncryptedImage,"field3"->imageId,"field4"->albumId, "field5"->signedAesEncryptedImageString))))
          result.foreach { response =>
          //println(s"Request completed in Send_createAlbum with status ${response.status} and content:\n${response.entity.asString}")
          writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")       
          master ! TrackHopsWhileLookUpOfEachFile(1) 
        }
      }

      case Send_getPicOfUserByImageId(authorId,actionUserId,imageId,albumId) =>
      { 
          master = sender
          val encryptedAESkey = getAccess(authorId,actionUserId, publicKey)
          if(!encryptedAESkey.isEmpty){
              val result = pipeline1(Get("http://localhost:8080/facebook/getPicOfUserByImageId",FormData(Seq("field1"->authorId, "field2"->actionUserId, "field3"->imageId, "field4"->albumId))))
              result.foreach { response =>

                 println(s"Request completed in Send_getPicOfUserByImageId with status ${response.status} and content:\n${response.entity.asString}")
                 writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
                 implicit val formats = DefaultFormats
                 val json = parse(response.entity.asString)
                 var imageContent1 = (json \ "imageContent").extract[String]

                 if(!(imageContent1.equals("Error"))){
                    var encryptedAesData = imageContent1
                    //println("\nencryptedAESkey before sending to decryptDataFromAPI : "+encryptedAESkey)
                    val decryptedAesData = decryptDataFromAPI(encryptedAesData, encryptedAESkey)              
                    println("\ndecryptedAesData inside Send_getPicOfUserByImageId: " + decryptedAesData)
                    val decodedBArray = Base64.decodeBase64(decryptedAesData)
                    val bos = new BufferedOutputStream(new FileOutputStream("images/imageofUser"+ actionUserId +".jpg"))
                    Stream.continually(bos.write(decodedBArray))
                    bos.close()
                }
                else
                println("this author did not post this post")          
            }
          }
         master ! TrackHopsWhileLookUpOfEachFile(1)  
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
          println("inside Send_GetPostOfUser....") 
          master = sender
          val result = pipeline1(Get("http://localhost:8080/facebook/getPostOfUser",FormData(Seq("field1"->authorId, "field2"->actionUserId))))
          result.foreach { response =>
           println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")      
        }
      }     

      case Send_GetPostOfUserByPostId(authorId,actionUserId,postId)=>
      { 
        println("inside Send_GetPostOfUserByPostId....") 
        master = sender
        val encryptedAESkey = getAccess(authorId,actionUserId, publicKey)
        println("encryptedAESkey :" + encryptedAESkey)
        if(!encryptedAESkey.isEmpty){
            
            val result = pipeline1(Get("http://localhost:8080/facebook/getPostOfUserByPostId",FormData(Seq("field1"->authorId, "field2"->actionUserId, "field3"->postId))))
            result.foreach { response =>
               //println(s"Request completed in Send_GetPostOfUserByPostId with status ${response.status} and content:\n${response.entity.asString}")
               writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
               implicit val formats = DefaultFormats
               val json = parse(response.entity.asString)
               //println("\njson = "+json)
               var content = (json \ "content").extract[String]
               //println("\ncontent = "+content)

               if(content.equals("ErrorNL")){
                 println("Not logged in!!")
               }else{
                    if(!(content.equals("Error"))){
                      var encryptedAesData = content
                      println("\nencryptedAESkey before sending to decryptDataFromAPI : "+encryptedAESkey)
                      println("encryptedAesData  :" + encryptedAesData)
                      val decryptedAesData = decryptDataFromAPI(encryptedAesData, encryptedAESkey)              
                      println("\ndecryptedAesData inside Send_GetPostOfUserByPostId: " + decryptedAesData)
                  }
                  else{
                  println("this author did not post this post")
                  }
               }              
            }
          }
          master ! TrackHopsWhileLookUpOfEachFile(1) 
               
        }


    case Send_SecureGetPostOfUserByPostId(authorId,actionUserId,postId)=>
      { 
        println("inside Send_SecureGetPostOfUserByPostId....") 
        val encryptedDataAndSignatureList = getSecureAccessToPost(authorId,actionUserId, publicKey,postId)

        if(!encryptedDataAndSignatureList.isEmpty){
          
          //var publicKeyExists : Boolean = publicKeyExist(authorId)
          
          val result = pipeline1(Get("http://localhost:8080/facebook/verifyPublicKeyOfUser/"+authorId))
          result.foreach { response =>
            //println(s"Request completed in Send_SecureGetPostOfUserByPostId with status ${response.status} and content:\n${response.entity.asString}")
            writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
            //Convert PublicKeyString to Byte Stream
            var publicKeyString = response.entity.asString
            var decoder : BASE64Decoder = new BASE64Decoder()
            var sigBytes2  : Array[Byte] = decoder.decodeBuffer(publicKeyString)

            // Convert the public key bytes into a PublicKey object
            var x509KeySpec : X509EncodedKeySpec = new X509EncodedKeySpec(sigBytes2)
            var keyFact : KeyFactory = KeyFactory.getInstance("RSA")
            var publicKeyUser : PublicKey = keyFact.generatePublic(x509KeySpec)

            //println("encryptedDataAndSignatureList : " + encryptedDataAndSignatureList)

            publicKeyMap += (authorId -> publicKeyUser)
            //println("publicKeyMap : "+publicKeyMap)

            var encryptedPost = encryptedDataAndSignatureList(0)
            var signature = encryptedDataAndSignatureList(1)

            var decryptedPost = RSA.decrypt(encryptedPost,privateKey)
            val encoder : BASE64Encoder  = new BASE64Encoder()
            val signatureString : String = encoder.encode(signature)
            var result = verifySignature(decryptedPost,signatureString,publicKeyUser)

            if(result){
              println("decryptedPost received : "+decryptedPost)
            }
            else{
              println("Sorry you are fake :P")
            }
          }
        }
        else
        {
          println("list is empty")
        }
        master ! TrackHopsWhileLookUpOfEachFile(1) 
      }

      case GiveAccess(actionUserId , publicKeyOfRequestor) => {
        actorRequestor = sender
        val result = pipeline1(Get("http://localhost:8080/facebook/verifyPublicKeyOfUser/"+actionUserId))
        result.foreach { response =>
           writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
           var publicKeyBytes : Array[Byte] = publicKeyOfRequestor.getEncoded()
           val encoder : BASE64Encoder  = new BASE64Encoder()
           val publicKeyString : String = encoder.encode(publicKeyBytes)
           if((response.entity.asString).equals(publicKeyString))
           {
              println("The public keys are equal. Hence the requestor is genuine.") 
              bytesArray  = RSA.encrypt(theAesKey,publicKeyOfRequestor)
              actorRequestor ! bytesArray
           }      
            else{
               actorRequestor ! Array[Byte]()
            }
        }
      }


      case GiveSecureAccessToPost(actionUserId , publicKeyOfRequestor, postId) => {
        actorRequestor = sender
        val result = pipeline1(Get("http://localhost:8080/facebook/verifyPublicKeyOfUser/"+actionUserId))
        result.foreach { response =>
        writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
           var publicKeyBytes : Array[Byte] = publicKeyOfRequestor.getEncoded()
           val encoder : BASE64Encoder  = new BASE64Encoder()
           val publicKeyString : String = encoder.encode(publicKeyBytes)
           if((response.entity.asString).equals(publicKeyString))
           {
              println("The public keys are equal. Hence the requestor is genuine.")
          
              val result = pipeline1(Get("http://localhost:8080/facebook/getPostOfUserByPostId",FormData(Seq("field1"->userId, "field2"->actionUserId, "field3"->postId))))
              result.foreach { response =>
              //println(s"Request completed in Send_GetPostOfUserByPostId/GiveSecureAccessToPost with status ${response.status} and content:\n${response.entity.asString}")
              writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}") 
              implicit val formats = DefaultFormats
              val json = parse(response.entity.asString)
              //println("\njson = "+json)
              var content = (json \ "content").extract[String]
              //println("\ncontent = "+content)
              if(content.equals("ErrorNL")){
                println("You are not logged in!")
              }else{
                    if(!(content.equals("Error"))){
                      var encryptedAesData = content
                      //println("\nencryptedAESkey before sending to decryptDataFromAPI : "+encryptedAESkey)

                      val decryptedAesData = Encryption.decrypt(theAesKey,encryptedAesData)              
                      println("\ndecryptedAesData inside Send_GetPostOfUserByPostId: " + decryptedAesData)

                      var arrayDataList = encryptAndSignData(decryptedAesData,publicKeyOfRequestor)
                      actorRequestor ! arrayDataList

                  }
                  else{
                  println("this author did not post this post")
                  actorRequestor ! List[Array[Byte]]()
                   //master ! TrackHopsWhileLookUpOfEachFile(1)
                  } 
              }
            }
          }      
            else{
               println("The public keys dont match")
               actorRequestor ! List[Array[Byte]]()
            }
        }

      }

    }

    def checkIfPublicKeyExists(userName:String) : Boolean = {
        //println("sessionMapOfUser" + sessionMapOfUser + "userName" + userName) 
        var publicKeyExist : Boolean = publicKeyMap.exists(_ == (userName,"Started"))
        println("publicKeyExist - " + publicKeyExist)
        if(publicKeyExist){
            return true
          }else{
            return false
          }
        }

    def encryptAndSignData(decryptedAesData : String , publicKeyOfRequestor : PublicKey) : List[Array[Byte]] = {

      val encryptedPostForRequestor : Array[Byte] = RSA.encrypt(decryptedAesData,publicKeyOfRequestor)
      //println("encryptedPostForRequestor : " + encryptedPostForRequestor)
      val signedDecryptedAesData : Array[Byte] = computeSignature(decryptedAesData)
      //println("signedDecryptedAesData :" + signedDecryptedAesData)
      //println("publicKeyOfRequestor" + publicKeyOfRequestor)
      val listOfEncryptedAndSignedData = List(encryptedPostForRequestor,signedDecryptedAesData)
      listOfEncryptedAndSignedData

    }

    def getAccess(authorId : String , actionUserId : String, publicKey : PublicKey) : Array[Byte] ={
      println("inside function getAccess...")
      val facebookUser_actor = system.actorSelection("akka://ClientSystem/user/FacebookAPISimulator/FacebookAPIClient:"+authorId)
      val future = facebookUser_actor ? GiveAccess(actionUserId , publicKey)
      val encryptedAESkey = Await.result(future, timeout.duration).asInstanceOf[Array[Byte]]
      encryptedAESkey
    }


    //more secure
    def getSecureAccessToPost(authorId : String , actionUserId : String, publicKey : PublicKey, postId :String ) : List[Array[Byte]] ={
      println("inside function getSecureAccessToPost ...")
      val facebookUser_actor = system.actorSelection("akka://ClientSystem/user/FacebookAPISimulator/FacebookAPIClient:"+authorId)
      val future = facebookUser_actor ? GiveSecureAccessToPost(actionUserId , publicKey, postId)
      val encryptedDataAndSignatureList = Await.result(future, timeout.duration).asInstanceOf[List[Array[Byte]]]
      encryptedDataAndSignatureList
    }

    def decryptDataFromAPI(encryptedAesData : String , encryptedAESkey : Array[Byte]) : String = {
        println("inside decryptDataFromAPI....for userName" + userId)
        val theAesKey = RSA.decrypt(encryptedAESkey,privateKey)
        println("inside decryptDataFromAPI....for userName1" + userId)
        var decryptedAesData = Encryption.decrypt(theAesKey, encryptedAesData)
        println("inside decryptDataFromAPI....for userName2" + userId)
        decryptedAesData
    }

    def secureRandomNumber(randomNumberString : String) : String = {
        
      val signature : Array[Byte] = computeSignature(randomNumberString)
      val encoder : BASE64Encoder  = new BASE64Encoder()
      val signatureString : String = encoder.encode(signature)
      signatureString 
    }

    def computeSignature(randomNumberString : String) : Array[Byte] =
    {
      var decoder : BASE64Decoder = new BASE64Decoder()
      var randomNumber  : Array[Byte] = decoder.decodeBuffer(randomNumberString) 
      var instance : Signature = Signature.getInstance("SHA256withRSA")
      instance.initSign(privateKey)
      instance.update(randomNumber)
      var signature : Array[Byte] = instance.sign()
      signature
    }

    def verifySignature(randomNumberString:String,signatureString:String,publicKey:PublicKey) : Boolean =
          {
            var decoder : BASE64Decoder = new BASE64Decoder()
            var randomNumber  : Array[Byte] = decoder.decodeBuffer(randomNumberString)
            var signature : Array[Byte] = decoder.decodeBuffer(signatureString)
            val signer : Signature = Signature.getInstance("SHA256withRSA")
            signer.initVerify(publicKey)
            signer.update(randomNumber)
            val bool : Boolean = signer.verify(signature)
            return bool
          }

    def secure_Connect(userCount : String, randomNumberString : String, signatureString : String) 
      {
        val result = pipeline2(Post("http://localhost:8080/facebook/secure_connect", FormData(Seq("field1"->userCount, "field2"->randomNumberString, "field3"->signatureString))))
        result.foreach { response =>
          println(s"Request in Secure_Connect completed with status ${response.status} and content:\n${response.entity.asString}")
          writeToLog(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        }
      }

  def writeToLog(content :String){ 
        var pw = new FileWriter("client_log.txt",true)
        pw.write(content)
        pw.close()
  }  

}

	








