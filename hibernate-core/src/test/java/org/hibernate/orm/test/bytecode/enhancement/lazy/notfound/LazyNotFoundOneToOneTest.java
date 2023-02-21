/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.notfound;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-12226")
@RunWith( BytecodeEnhancerRunner.class )
public class LazyNotFoundOneToOneTest extends BaseCoreFunctionalTestCase {
	private static int ID = 1;

	private SQLStatementInterceptor sqlInterceptor;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Lazy.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);
		sqlInterceptor = new SQLStatementInterceptor( configuration );
	}

	@Test
	public void test() {
		doInHibernate(
				this::sessionFactory, session -> {
					Lazy p = new Lazy();
					p.id = ID;
					User u = new User();
					u.id = ID;
					u.setLazy( p );
					session.persist( u );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( session.get( Lazy.class, ID ) );
				}
		);

		sqlInterceptor.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					User user = session.find( User.class, ID );

					// `@NotFound` forces EAGER join fetching
					assertThat( sqlInterceptor.getQueryCount() ).
							describedAs( "Expecting 2 queries due to `@NotFound`" )
							.isEqualTo( 2 );
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
		@LazyToOne(value = LazyToOneOption.NO_PROXY)
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
