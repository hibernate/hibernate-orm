/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.notfound;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-12226")
@DomainModel(
		annotatedClasses = {
				LazyNotFoundOneToOneNonUpdatableNonInsertableTest.User.class,
				LazyNotFoundOneToOneNonUpdatableNonInsertableTest.Lazy.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyNotFoundOneToOneNonUpdatableNonInsertableTest {
	private static int ID = 1;

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					Lazy p = new Lazy();
					p.id = ID;
					User u = new User();
					u.id = ID;
					u.setLazy( p );
					session.persist( u );
				}
		);

		scope.inTransaction( session -> session.remove( session.get( Lazy.class, ID ) ) );

		scope.inTransaction( session -> {
					User user = session.find( User.class, ID );
					assertThat( Hibernate.isPropertyInitialized( user, "lazy" ) )
							.describedAs( "Expecting `User#lazy` to be bytecode initialized due to `@NotFound`" )
							.isTrue();
					assertThat( user.getLazy() )
							.describedAs( "Expecting `User#lazy` to be null due to `NotFoundAction#IGNORE`" )
							.isNull();
				}
		);
	}

	@Entity(name="User")
	@Table(name = "USER_TABLE")
	public static class User {

		@Id
		private Integer id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "id",
				referencedColumnName = "id",
				insertable = false,
				updatable = false
		)
		private Lazy lazy;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Lazy getLazy() {
			return lazy;
		}

		public void setLazy(Lazy lazy) {
			this.lazy = lazy;
		}

	}

	@Entity(name = "Lazy")
	@Table(name = "LAZY")
	public static class Lazy {
		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
