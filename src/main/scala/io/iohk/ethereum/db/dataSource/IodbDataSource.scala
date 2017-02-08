package io.iohk.ethereum.db.dataSource

import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

import io.iohk.iodb.{ByteArrayWrapper, LSMStore}

class IodbDataSource private (lSMStore: LSMStore, keySize: Int, path: String) extends DataSource {

  import IodbDataSource._

  override def get(namespace: Namespace, key: Key): Option[Value] = {
    require(namespace.length + key.length == keySize, "Wrong key size in IODB get")
    lSMStore.get(ByteArrayWrapper((namespace ++ key).toArray)).map(v => v.data.toIndexedSeq)
  }

  override def update(namespace: Namespace, toRemove: Seq[Key], toUpsert: Seq[(Key, Value)]): DataSource = {
    require(
      toRemove.forall{ keyToRemove => namespace.length + keyToRemove.length == keySize } &&
        toUpsert.forall{ case (keyToUpSert, _) => namespace.length + keyToUpSert.length == keySize },
      "Wrong key size in IODB update"
    )
    lSMStore.update(
      ByteArrayWrapper(storageVersionGen()),
      toRemove.map(key => ByteArrayWrapper((namespace ++ key).toArray)),
      asStorables(namespace, toUpsert))
    new IodbDataSource(lSMStore, keySize, path)
  }

  override def clear: DataSource = {
    destroy()
    IodbDataSource(path, keySize, recreate = true)
  }

  override def close(): Unit = lSMStore.close()

  override def destroy(): Unit = {
    close()
    val directoryDeletionSuccess = deleteDirectory(new File(path))
    assert(directoryDeletionSuccess, "Iodb folder destruction failed")
  }

  private def asStorables(namespace: Namespace,
                          keyValues: Seq[(Key, Value)]): Seq[(ByteArrayWrapper, ByteArrayWrapper)] =
    keyValues.map(kv => ByteArrayWrapper((namespace ++ kv._1).toArray) -> ByteArrayWrapper(kv._2.toArray))
}


object IodbDataSource {
  private val updateCounter = new AtomicLong(System.currentTimeMillis())

  private def storageVersionGen(): Array[Byte] = {
    ByteBuffer.allocate(java.lang.Long.SIZE / java.lang.Byte.SIZE).putLong(updateCounter.incrementAndGet()).array()
  }

  /**
    * This function constructs an IodbDataSource.
    *
    * @param path of the folder where the DataSource files will be stored.
    * @param keySize of the keys to be stored in the DataSource.
    *                This length includes the length of the namespace and the length of the keys inside this namespace
    * @param recreate boolean that, if set to true, this function will return a DataSource with no data in it.
    *                 If set to false the returned DataSource will continue to use previously built DataSource files.
    * @return an IodbDataSource.
    */
  def apply(path: String, keySize: Int, recreate: Boolean): IodbDataSource = {
    val dir: File = new File(path)
    val dirSetupSuccess =
      if(recreate) (!dir.exists() || deleteDirectory(dir)) && dir.mkdirs()
      else dir.exists() && dir.isDirectory
    assert(dirSetupSuccess, "Iodb folder creation failed")

    val lSMStore: LSMStore = new LSMStore(dir = dir, keySize = keySize, keepSingleVersion = true)
    new IodbDataSource(lSMStore, keySize, path)
  }

  private def deleteDirectory(dir: File): Boolean = {
    require(dir.exists() && dir.isDirectory, "Trying to delete a file thats not a folder")
    val files = Option(dir.listFiles()).getOrElse(Array())
    val filesDeletionSuccess: Boolean = files.map { f =>
      if(f.isDirectory) deleteDirectory(f) else f.delete()
    }.forall(identity)
    filesDeletionSuccess && dir.delete()
  }
}
