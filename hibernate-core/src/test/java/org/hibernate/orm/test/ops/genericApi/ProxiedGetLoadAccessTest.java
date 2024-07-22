/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops.genericApi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Proxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = ProxiedGetLoadAccessTest.UserImpl.class
)
@SessionFactory
public class ProxiedGetLoadAccessTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create a row
		scope.inTransaction(
				session ->
						session.persist( new UserImpl( "steve" ) )
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `get` access
		scope.inTransaction(
				session -> {
					// THis technically works
					session.get( UserImpl.class, 1 );
					session.get( User.class, 1 );
				}
		);

		scope.inTransaction(
				session -> {
					session.get( UserImpl.class, 1, LockMode.PESSIMISTIC_WRITE );
					session.get( User.class, 1, LockMode.PESSIMISTIC_WRITE );
				}
		);

		scope.inTransaction(
				session -> {
					session.get( UserImpl.class, 1, LockOptions.UPGRADE );
					session.get( User.class, 1, LockOptions.UPGRADE );
				}
		);

		scope.inTransaction(
				session -> {
					session.byId( UserImpl.class ).load( 1 );
					session.byId( User.class ).load( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					session.byId( UserImpl.class ).with( LockOptions.UPGRADE ).load( 1 );
					session.byId( User.class ).with( LockOptions.UPGRADE ).load( 1 );
				}
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `load` access
		scope.inTransaction(
				session -> {
					session.load( UserImpl.class, 1 );
					session.load( User.class, 1 );
				}
		);

		scope.inTransaction(
				session -> {
					session.load( UserImpl.class, 1, LockMode.PESSIMISTIC_WRITE );
					session.load( User.class, 1, LockMode.PESSIMISTIC_WRITE );
				}
		);
		scope.inTransaction(
				session -> {
					session.load( UserImpl.class, 1, LockOptions.UPGRADE );
					session.load( User.class, 1, LockOptions.UPGRADE );
				}
		);

		scope.inTransaction(
				session -> {
					session.byId( UserImpl.class ).getReference( 1 );
					session.byId( User.class ).getReference( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					session.byId( UserImpl.class ).with( LockOptions.UPGRADE ).getReference( 1 );
					session.byId( User.class ).with( LockOptions.UPGRADE ).getReference( 1 );
				}
		);
	}

	public interface User {
		Integer getId();

		String getName();

		void setName(String name);
	}

	@Entity(name = "User")
	@Table(name = "my_user")
	@Proxy(proxyClass = User.class)
	public static class UserImpl implements User {
		private Integer id;
		private String name;

		public UserImpl() {
		}

		public UserImpl(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		@Override
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}
}
