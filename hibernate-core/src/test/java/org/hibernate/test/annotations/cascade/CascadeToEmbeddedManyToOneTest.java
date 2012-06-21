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
package org.hibernate.test.annotations.cascade;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;


public class CascadeToEmbeddedManyToOneTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testPersistCascadeToSetOfEmbedded() {
		Session sess = openSession();
		try {
			final Transaction trx  = sess.beginTransaction();
			try {
				final Set<PersonPair> setOfPairs = new HashSet<PersonPair>();
				setOfPairs.add(new PersonPair(new Person("PERSON NAME 1"), new Person("PERSON NAME 2")));
				sess.persist( new CodedPairSetHolder( "CODE", setOfPairs ) );
				sess.flush();
			} finally {
				trx.rollback();
			}
		} finally {
			sess.close();
		}
	}

	@Test
	public void testPersistCascadeToEmbedded() {
		Session sess = openSession();
		try {
			final Transaction trx  = sess.beginTransaction();
			try {
				PersonPair personPair = new PersonPair(new Person("PERSON NAME 1"), new Person("PERSON NAME 2"));
				sess.persist( new CodedPairHolder( "CODE", personPair ) );
				sess.flush();
			} finally {
				trx.rollback();
			}
		} finally {
			sess.close();
		}
	}
	
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				CodedPairSetHolder.class,
				CodedPairHolder.class,
				Person.class,
				PersonPair.class
		};
	}
}
