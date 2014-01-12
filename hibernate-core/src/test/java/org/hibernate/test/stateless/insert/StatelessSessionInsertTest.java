/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
    private static final Logger log = Logger.getLogger(StatelessSessionInsertTest.class);

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
