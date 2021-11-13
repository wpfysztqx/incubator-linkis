/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.linkis.engineconn.acessible.executor.log

import java.util
import java.util.concurrent.TimeUnit

import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.engineconn.acessible.executor.conf.AccessibleExecutorConfiguration
import org.apache.linkis.engineconn.acessible.executor.listener.LogListener
import org.apache.linkis.engineconn.acessible.executor.listener.event.TaskLogUpdateEvent
import org.apache.linkis.engineconn.core.EngineConnObject
import scala.collection.JavaConversions._

object LogHelper extends Logging {


  val logCache = new MountLogCache(AccessibleExecutorConfiguration.ENGINECONN_LOG_CACHE_NUM.getValue)

  private var logListener: LogListener = _

  private val CACHE_SIZE = AccessibleExecutorConfiguration.ENGINECONN_LOG_SEND_SIZE.getValue

  def setLogListener(logListener: LogListener): Unit = this.logListener = logListener

  def pushAllRemainLogs(): Unit = {
    //    logger.info(s"start to push all remain logs")
    Thread.sleep(30)
    //logCache.synchronized{
    if (logListener == null) {
      warn("logListener is null, can not push remain logs")
      //return
    } else {
      var logs: util.List[String] = null
      logCache.synchronized {
        logs = logCache.getRemain
      }
      if (logs != null && logs.size > 0) {
        val sb: StringBuilder = new StringBuilder
        import scala.collection.JavaConversions._
        logs map (log => log + "\n") foreach sb.append
        logListener.onLogUpdate(TaskLogUpdateEvent(null, sb.toString))
      }
    }
    logger.info("end to push all remain logs")
  }

  def dropAllRemainLogs(): Unit = {
    var logs: util.List[String] = null
    logCache.synchronized {
      logs = logCache.getRemain
    }
    if (null != logs && logs.size() > 0) {
      logger.info(s"Dropped ${logs.size()} remained logs.")
    }
  }

  Utils.defaultScheduler.scheduleAtFixedRate(new Runnable {
    override def run(): Unit = Utils.tryAndWarn {

      if (logListener == null || logCache == null) {
        info("logCache or logListener is null")
        return
      } else {
        if (logCache.size > CACHE_SIZE) {
          val logs = logCache.getRemain
          val sb = new StringBuilder

          for (log <- logs) {
            sb.append(log).append("\n")
          }
          if (EngineConnObject.isReady) {
            logListener.onLogUpdate(TaskLogUpdateEvent(null, sb.toString))
          }
        }
      }
    }
  }, 60 * 1000, AccessibleExecutorConfiguration.ENGINECONN_LOG_SEND_TIME_INTERVAL.getValue, TimeUnit.MILLISECONDS)


}
