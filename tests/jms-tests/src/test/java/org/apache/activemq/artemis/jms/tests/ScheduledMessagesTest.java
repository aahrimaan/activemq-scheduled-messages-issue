/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.tests;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.tests.util.ProxyAssertSupport;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.Map;

public class ScheduledMessagesTest extends JMSTestCase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   Connection conn = null;

   @Override
   @After
   public void tearDown() throws Exception {
      try {
         if (conn != null) {
            conn.close();
            conn = null;
         }
      } finally {
         super.tearDown();
      }
   }

   @Test
   public void testPersistent_Scheduled() throws Exception {
      conn = createConnection();

      Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageProducer prod = session.createProducer(queue1);
      prod.setDeliveryMode(DeliveryMode.PERSISTENT);

      TextMessage m = session.createTextMessage("message one");
      long time = System.currentTimeMillis() + 5000;
      m.setLongProperty("_AMQ_SCHED_DELIVERY",  time);

      prod.send(m);
      logger.info("Sent" + m);
      conn.close();

      Map<String, Object>[] beforeRestart = ActiveMQServerTestCase.servers.get(0).listScheduledMessages(queue1.getQueueName());
      logger.info("_AMQ_SCHED_DELIVERY =  " + beforeRestart[0].get("_AMQ_SCHED_DELIVERY"));
      ProxyAssertSupport.assertEquals(time, beforeRestart[0].get("_AMQ_SCHED_DELIVERY"));

      ActiveMQServerTestCase.servers.get(0).stop();

      ActiveMQServerTestCase.servers.get(0).start(getConfiguration(), false);


      Map<String, Object>[] afterRestart = ActiveMQServerTestCase.servers.get(0).listScheduledMessages(queue1.getQueueName());
      logger.info("_AMQ_SCHED_DELIVERY =  " + afterRestart[0].get("_AMQ_SCHED_DELIVERY"));
//      ProxyAssertSupport.assertEquals(time, afterRestart[0].get("_AMQ_SCHED_DELIVERY"));

      conn = createConnection();

      session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer cons = session.createConsumer(queue1);

      conn.start();

      TextMessage rm = (TextMessage) cons.receive(10000);
      logger.info("Received" + rm);
      ProxyAssertSupport.assertEquals("message one", rm.getText());
   }

}
