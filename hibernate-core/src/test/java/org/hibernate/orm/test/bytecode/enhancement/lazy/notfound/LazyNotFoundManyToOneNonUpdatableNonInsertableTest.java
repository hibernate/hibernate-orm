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
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-12226")
@DomainModel(
		annotatedClasses = {
				LazyNotFoundManyToOneNonUpdatableNonInsertableTest.User.class,
				LazyNotFoundManyToOneNonUpdatableNonInsertableTest.Lazy.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyNotFoundManyToOneNonUpdatableNonInsertableTest {
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
					// per UserGuide (and simply correct behavior), `@NotFound` forces EAGER fetching
					assertThat( Hibernate.isPropertyInitialized( user, "lazy" ) )
							.describedAs( "`User#lazy` is not eagerly initialized due to presence of `@NotFound`" )
							.isTrue();
					assertNull( user.getLazy() );
				}
		);
	}

	@Entity(name="User")
	@Table(name = "USER_TABLE")
	public static class User {

		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
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
