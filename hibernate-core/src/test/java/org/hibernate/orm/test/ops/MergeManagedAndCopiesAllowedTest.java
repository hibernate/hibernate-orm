/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertSame;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A  1 ------------> 1 B 1 ----------> 1 C
 *                      1                 1
 *                      |                 |
 *                      |                 |
 *                      V                 V
 *                      1                 N
 *                      D 1------------>N E
 *
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				MergeManagedAndCopiesAllowedTest.A.class,
				MergeManagedAndCopiesAllowedTest.B.class,
				MergeManagedAndCopiesAllowedTest.C.class,
				MergeManagedAndCopiesAllowedTest.D.class,
				MergeManagedAndCopiesAllowedTest.E.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "allow"))
public class MergeManagedAndCopiesAllowedTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		A a = new A();
		a.b = new B();
		a.b.d = new D();
		a.b.d.dEs.add( new E() );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		scope.inTransaction(
				session -> {
					A aGet = session.get( A.class, a.id );
					aGet.b.c = new C();
					Set<E> copies = new HashSet<>();
					for ( E e : aGet.b.d.dEs ) {
						copies.add( new E( e.id, "description" ) );
					}
					aGet.b.c.cEs.addAll( copies );
					session.merge( aGet );
				}
		);

		scope.inTransaction(
				session -> {
					A aGet = session.get( A.class, a.id );
					E e = aGet.b.c.cEs.iterator().next();
					assertSame( e, aGet.b.d.dEs.iterator().next() );
					assertThat( e.description, is( "description" ) );
				}
		);
	}

	@Entity(name = "A")
	public static class A {
		@Id
		@GeneratedValue
		private int id;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
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

		E() {
		}

		E(int id, String description) {
			this.id = id;
			this.description = description;
		}
	}
}
