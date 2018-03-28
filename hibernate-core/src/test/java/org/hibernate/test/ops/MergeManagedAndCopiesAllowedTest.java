/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * A  1 ------------> 1 B 1 ----------> 1 C
 *                      1                 1
 *                      |                 |
 *                      |                 |
 *                      V                 V
 *                      1                 N
 *                      D 1------------>N E
 *
 *
 * @author Gail Badner
 */
public class MergeManagedAndCopiesAllowedTest extends BaseCoreFunctionalTestCase {

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				A.class,
				B.class,
				C.class,
				D.class,
				E.class
		};
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.event.merge.entity_copy_observer", "allow"  );
	}

	@Test
	public void testIt() {
		A a = new A();
		a.b = new B();
		a.b.d = new D();
		a.b.d.dEs.add( new E() );

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( a );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					A aGet= session.get( A.class, a.id );
					aGet.b.c = new C();
					Set<E> copies = new HashSet<>();
					for ( E e : aGet.b.d.dEs ) {
						copies.add ( new E( e.id, "description" ) );
					}
					aGet.b.c.cEs.addAll( copies );
					session.merge( aGet );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					A aGet= session.get( A.class, a.id );
					E e = aGet.b.c.cEs.iterator().next();
					assertSame( e, aGet.b.d.dEs.iterator().next() );
					assertEquals( "description", e.description );
				}
		);
	}

	@Entity(name = "A")
	public static class A {
		@Id
		@GeneratedValue
		private int id;

		@OneToOne(fetch=FetchType.EAGER, cascade = CascadeType.ALL)
		private B b;
	}

	@Entity(name = "B")
	public static class B {
		@Id
		@GeneratedValue
		private int id;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		private C c;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		private D d;
	}

	@Entity(name = "C")
	public static class C {
		@Id
		@GeneratedValue
		private int id;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(nullable = true)
		private Set<E> cEs = new HashSet<>();
	}


	@Entity(name = "D")
	public static class D {
		@Id
		@GeneratedValue
		private int id;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(nullable = false)
		private Set<E> dEs = new HashSet<>();
	}

	@Entity(name = "E")
	public static class E {
		@Id
		@GeneratedValue
		private int id;

		private String description;

		E() {}

		E(int id, String description) {
			this.id = id;
			this.description = description;
		}
	}
}
