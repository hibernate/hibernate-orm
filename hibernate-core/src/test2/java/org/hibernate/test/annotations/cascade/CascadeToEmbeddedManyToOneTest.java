/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;


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
