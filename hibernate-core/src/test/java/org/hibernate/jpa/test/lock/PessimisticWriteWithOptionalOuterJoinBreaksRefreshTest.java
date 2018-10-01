/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.jpa.test.lock;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-13000")
public class PessimisticWriteWithOptionalOuterJoinBreaksRefreshTest extends BaseEntityManagerFunctionalTestCase {

	@Entity
	public static class A {
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;
	}

	@Entity
	public static class B {
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@ManyToOne( cascade = { CascadeType.PERSIST } )
		@JoinTable( name = "test" )
		A a;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { A.class, B.class };
	}

	private B b;

	@Before
	public void setUp() throws Exception {
		doInJPA( this::entityManagerFactory, em -> {
			b = new B();
			b.a = new A();
			em.persist( b );
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-13000", message = "Fails due to regression in HHH-12257" )
	public void pessimisticWriteWithOptionalOuterJoinBreaksRefreshTest() {
		doInJPA( this::entityManagerFactory, em -> {
			b = em.find( B.class, b.id );
			em.lock( b, LockModeType.PESSIMISTIC_WRITE );
			em.flush();
			em.refresh( b );
		} );
	}

}
