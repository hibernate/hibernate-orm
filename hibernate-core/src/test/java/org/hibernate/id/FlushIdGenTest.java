/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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

package org.hibernate.id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-8611")
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class FlushIdGenTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testPersistBeforeTransaction() {
		Session session = openSession();
		RootEntity ent1_0 = new RootEntity();
		RootEntity ent1_1 = new RootEntity();

		session.persist( ent1_0 );
		session.persist( ent1_1 );

		Transaction tx = session.beginTransaction();
		tx.commit(); //flush
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				RootEntity.class,
				RelatedEntity.class,
		};
	}

}
