package io.enigma.downloadily

import java.util.concurrent.{ExecutorService, Executors, ThreadPoolExecutor}

object Config {

  val DEFAULT_JAVALIN_THREAD_POOL_THREAD_MAX = 10
  val DEFAULT_JAVALIN_DOWNLOADER_THREAD_POOL_MAX = 3
  val DEFAULT_THREAD_POOL_ENV_VAR = "MAIN_POOL_MAX_THREADS"
  val DOWNLOAD_THREAD_POOL_ENV_VAR = "DL_POOL_MAX_THREADS"


  val JAVALIN_THREAD_POOL: ExecutorService = Executors.newFixedThreadPool(sys.env.getOrElse(DEFAULT_THREAD_POOL_ENV_VAR, DEFAULT_JAVALIN_THREAD_POOL_THREAD_MAX).asInstanceOf[Int])
  val JAVALIN_DOWNLOADER_THREAD_POOL: ExecutorService = Executors.newFixedThreadPool(sys.env.getOrElse(DOWNLOAD_THREAD_POOL_ENV_VAR, DEFAULT_JAVALIN_DOWNLOADER_THREAD_POOL_MAX).asInstanceOf[Int])

  sealed class ThreadPoolIsFullException extends Exception {}

  def isThreadPoolFull(pool : ThreadPoolExecutor) : Boolean = {
   pool.getActiveCount >= pool.getMaximumPoolSize
  }
}
