/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
 package org.hibernate.test.typedescriptor;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

 /**
  * @author Strong Liu
  */
public class CharInNativeQueryTest extends BaseCoreFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[] {
                Issue.class
        };
    }
    @Test
    @TestForIssue(jiraKey = "HHH-2304")
    public void testNativeQuery() {
        Issue issue = new Issue();
        issue.setIssueNumber( "HHH-2304" );
        issue.setDescription( "Wrong type detection for sql type char(x) columns" );

        Session session = openSession();
        session.beginTransaction();
        session.persist( issue );
        session.getTransaction().commit();
        session.close();

        session = openSession(  );
        session.beginTransaction();
        Object issueNumber = session.createSQLQuery( "select issue.issueNumber from Issue issue" ).uniqueResult();
        session.getTransaction().commit();
        session.close();

        assertNotNull( issueNumber );
        assertTrue( issueNumber instanceof String );
        assertEquals( "HHH-2304", issueNumber );


    }

 }
