/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.notfound;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
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
import jakarta.persistence.OneToOne;
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
				LazyNotFoundOneToOneTest.User.class,
				LazyNotFoundOneToOneTest.Lazy.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyNotFoundOneToOneTest {
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
					SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getSessionFactory()
							.getSessionFactoryOptions().getStatementInspector();
					statementInspector.clear();

					User user = session.find( User.class, ID );

					// `@NotFound` forces EAGER join fetching
					statementInspector.assertExecutedCount(2);
					assertThat( Hibernate.isPropertyInitialized( user, "lazy" ) )
							.describedAs( "Expecting `User#lazy` to be eagerly fetched due to `@NotFound`" )
							.isTrue();
					assertThat( Hibernate.isInitialized( user.getLazy() ) )
							.describedAs( "Expecting `User#lazy` to be eagerly fetched due to `@NotFound`" )
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
