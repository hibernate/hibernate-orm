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
package org.hibernate.test.annotations.join.namingstrategy;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Sergey Vasilyev
 */
public class NamingStrategyJoinTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new TestNamingStrategy() );
	}


	@Test
	public void testJoinToSecondaryTable() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Life life = new Life();
		life.duration = 15;
		life.fullDescription = "Long long description";
		s.persist( life );
		tx.commit();
		s.close();

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Life.class,
				SimpleCat.class
		};
	}

}
