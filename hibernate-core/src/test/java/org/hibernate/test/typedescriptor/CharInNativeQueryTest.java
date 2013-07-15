/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
