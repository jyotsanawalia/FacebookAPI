package FacebookAPI

// import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
// import scala.collection.mutable._
// import spray.http._
// import spray.client.pipelining._
// import scala.util.Random
// import scala.concurrent.ExecutionContext
// //client stuff
// import akka.actor._
// //import common._
// import akka.util._
// import scala.concurrent.duration._
// import akka.routing.RoundRobinRouter
// import java.net.InetAddress

//import scala.concurrent.ExecutionContext.Implicits.global

//for image
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

//for image compression
import java.io.FileInputStream
import javax.imageio.ImageIO
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageWriter
import java.awt.image.BufferedImage
import javax.imageio.ImageWriteParam
import javax.imageio.stream.ImageOutputStream
import java.io.OutputStream
import java.io.InputStream
import javax.imageio.IIOImage
import java.util.Iterator
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

//for simple copy not limited to images
import java.io.{File,FileInputStream,FileOutputStream}

object FacebookHelper 
{
    def main(args: Array[String])
  {
	  
    //Code to copy an image from one location to another location
    // val photo1 = ImageIO.read(new File("photo.jpg"))
    // val photo2 = phototest(photo1) 
    // ImageIO.write(photo2, "jpg", new File("test.jpg"))
    // ImageIO.write(photo1, "jpg", new File("test1.jpg"))

    // //code to compress and then copy
    // val imageFile = new File("photo.jpg")
    // compressImage(imageFile,imageFile.getName,0.1f)
    // val photo1 = ImageIO.read(imageFile)
    // val photo2 = phototest(photo1) 
    // ImageIO.write(photo2, "jpg", new File("test.jpg"))
    // ImageIO.write(photo1, "jpg", new File("test1.jpg"))

    //code to directly copy files
    val src = new File("photo.jpg")
    val dest = new File("test.jpg")
    new FileOutputStream(dest) getChannel() transferFrom(
    new FileInputStream(src) getChannel, 0, Long.MaxValue )

	  //val client_actor =system.actorOf(Props(new FacebookAPISimulator(system,numOfUsers)),name="ClientActor")
	  //client_actor ! Start(system)
  }

    def phototest(img: BufferedImage): BufferedImage = {
    // obtain width and height of image
    val w = img.getWidth
    val h = img.getHeight
    // create new image of the same size
    val out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    // copy pixels (mirror horizontally)
    for (x <- 0 until w)
      for (y <- 0 until h)
        out.setRGB(x, y, img.getRGB(w - x - 1, y) & 0xffffff)   
    // draw red diagonal line
    for (x <- 0 until (h min w))
      out.setRGB(x, x, 0xff0000)

    out
  }

  def compressImage(file: File, filename: String, qualityOfOutPutImage: Float): InputStream = {
    val inputStream = new FileInputStream(file)
     
    // Creating An In Memory Output Stream 
    val outPutStream = new ByteArrayOutputStream
     
    val image = ImageIO.read(inputStream)   // BufferedImage
     
    val writers = ImageIO.getImageWritersByFormatName("jpg")
    val writer = writers.next
    val imageOutputStream = ImageIO.createImageOutputStream(outPutStream) // Image Output Stream
    writer.setOutput(imageOutputStream)
     
    val param = writer.getDefaultWriteParam
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) // Setting Compression Mode
     
    // Specifying The Image Quality , We Can Choose The Quality Required
    param.setCompressionQuality(qualityOfOutPutImage) 
     
    writer.write(null, new IIOImage(image, null, null), param)
     
    // Closing The Input and Output Streams
    inputStream.close
    outPutStream.close
    imageOutputStream.close
    writer.dispose                       // Disposing writer
     
    // Creating The InputStream From ByteArrayInputStream
    val fileInputStream: InputStream = new ByteArrayInputStream(outPutStream.toByteArray)
    fileInputStream    // Returned The Compressed Image Input Stream 
  }

}










