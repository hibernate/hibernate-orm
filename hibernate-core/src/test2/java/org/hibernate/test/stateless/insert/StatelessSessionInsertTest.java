/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stateless.insert;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * @author mukhanov@gmail.com
 */
public class StatelessSessionInsertTest extends BaseCoreFunctionalTestCase {

    @Override
    public String[] getMappings() {
        return new String[]{"stateless/insert/Mappings.hbm.xml"};
    }

    @Test
    public void testInsertWithForeignKey() {
        Session session = sessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        Message msg = new Message();
        final String messageId = "message_id";
        msg.setId(messageId);
        msg.setContent("message_content");
        msg.setSubject("message_subject");
        session.save(msg);

        tx.commit();
        session.close();

        StatelessSession statelessSession = sessionFactory().openStatelessSession();
        tx = statelessSession.beginTransaction();

        MessageRecipient signature = new MessageRecipient();
        signature.setId("recipient");
        signature.setEmail("recipient@hibernate.org");
        signature.setMessage(msg);
        statelessSession.insert(signature);

        tx.commit();

        cleanup();
    }

    private void cleanup() {
        Session s = openSession();
        s.beginTransaction();
        s.createQuery("delete MessageRecipient").executeUpdate();
        s.createQuery("delete Message").executeUpdate();
        s.getTransaction().commit();
        s.close();
    }
}
