/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableWithPluralAttributeTest.A.class,
				EmbeddableWithPluralAttributeTest.C.class
		}
)
@SessionFactory
public class EmbeddableWithPluralAttributeTest {


	@BeforeAll
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					A a = new A();
					a.id = 1;

					B b = B.buildB();
					a.b = b;

					session.persist( a );
				}
		);
	}

	@Test
	public void testAsParameterReuseInWhereClauseZeroResults(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select a from A a where a.b = :b" ).
							setParameter( "b", new B() ).list();
					assertThat( results.size(), is( 0 ) );
				}
		);
	}

	@Test
	public void testAsParameterReuseInWhereClause(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select a from A a where a.b = :b" ).
							setParameter( "b", B.buildB() ).list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private Integer id;

		private B b;

	}

	@Embeddable
	public static class B {
		@Column(name = "val")
		private String value;

		@OneToMany(cascade = CascadeType.ALL)
		private List<C> cs;

		public static B buildB() {
			B b = new B();
			b.value = "v";
			C c = new C();

			c.id = 2L;
			c.name = "c";

			List cs = new ArrayList();
			cs.add( c );
			b.cs = cs;

			return b;
		}
	}

	@Entity(name = "C")
	public static class C {
		@Id
		private Long id;

		private String name;
	}


}
