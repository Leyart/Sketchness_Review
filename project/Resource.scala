import sbt._

import java.net.URL
import java.io.File
import java.io.IOException

/** Resource handler
  *
  * Downloads and eventually unzips a resource or simply
  * creates a directory, deletes them when required.
  *
  * @constructor Hidden, use factory pattern with the companion object.
  *
  * @param name A descriptive name for the resource
  * @param path The local path of the resource
  * @param url The (valid) remote url of the resource, if null only local dir is created
  * @param zipped Flag to mark if the resource is zipped (and has to be unzipped)
  */
class Resource private (name: String, path :String, url :String = null, zipped :Boolean = true) {

	private val _path :File = new File(path)
	private val _url :URL = if(url != null) new URL(url) else null

	/** Retrieves the resource
	  *
	  * Acts only if the resource doesn't already exist
	  * to force: delete dir manually o with [[Resource.clean]]
	  *
	  * @param log The logger to send message to
	  */
	def get(log :Logger = ConsoleLogger()) = {
		log.info("Retrieving " + name + ".")
		if(!_path.exists()) {
			try {
				if(_url == null) _path.mkdirs()
				else if(zipped) IO.unzipURL(_url, _path)
				else IO.download(_url, _path)
				log.success("-> Resource retrieved.")
			} catch {
				case e: Exception => {
					log.error("-> Unable to get resource!")
					e.printStackTrace()
				}
			}
		} else {
			log.info("-> Resource already existent.")
		}
	}

	/** Delete the resource
	  *
	  * WARNING: Be careful, this will phisically remove the resource files
	  *
	  * @param log The logger to send message to
	  */
	def clean(log :Logger = ConsoleLogger()) = {
		log.info("Deleting " + name + ".")
		if(_path.exists()) {
			try {
				delete(_path)
				log.success("-> Resource deleted.")
			} catch {
				case e :IOException => {
					log.error("-> Unable to delete resource!")
					e.printStackTrace()
				}
			}
		} else {
			log.info("-> Resource already non existent.")
		}
	}

	/** Recursively deletes folder tree
	  *
	  * @param file The file or the folder to delete
	  */
	private def delete(file :File) :Unit = {
		if(file.isDirectory()) file.listFiles() foreach { delete(_) }
		file.delete();
	}

	override def toString() = "Resource(name: \"" + name + "\", path: \"" + path + "\", url: \"" + url + "\")"
}

/** Factory for [[Resource]] instances. */
object Resource {

	/** Creates a new resouce
	  * @see [[Reource]] constructor
	  */
	def apply(name :String, path :String, url :String = null, zipped :Boolean = true) = new Resource(name, path, url, zipped)
}
