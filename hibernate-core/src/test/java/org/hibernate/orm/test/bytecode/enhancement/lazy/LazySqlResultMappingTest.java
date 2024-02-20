/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.List;

import org.hibernate.Hibernate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Jan Schatteman
 * @author Christian Beikov
 */
@RunWith( BytecodeEnhancerRunner.class )
public class LazySqlResultMappingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ User.class };
	}

	@Before
	public void prepareData() {
		doInHibernate( this::sessionFactory,
					session -> {
						session.persist(new User(1L, (byte)1));
					}
		);
	}

	@After
	public void cleanupData() {
		doInHibernate( this::sessionFactory,
					   session -> {
						   session.createMutationQuery("delete from User").executeUpdate();
					   }
		);
	}

	@Test
	public void testGetIdAndPrincipalUsingFieldResults() {
		doInHibernate( this::sessionFactory,
					session -> {
							List<User> users = session.createNamedQuery( "getIdAndPrincipalUsingFieldResults", User.class ).getResultList();
							Assertions.assertTrue( Hibernate.isPropertyInitialized( users.get(0), "principal" ) );
					}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-953" )
	public void testGetIdAndPrincipalWithoutUsingFieldResults() {
		doInHibernate( this::sessionFactory,
					session -> {
						List<User> users = session.createNamedQuery( "getIdAndPrincipalWithoutUsingFieldResults", User.class ).getResultList();
						Assertions.assertTrue( Hibernate.isPropertyInitialized( users.get(0), "principal" ) );
					}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15667" )
	public void testGetIdWithoutUsingFieldResults() {
		doInHibernate( this::sessionFactory,
					   session -> {
						   List<User> users = session.createNamedQuery( "getIdWithoutUsingFieldResults", User.class ).getResultList();
						   Assertions.assertFalse( Hibernate.isPropertyInitialized( users.get(0), "principal" ) );
					   }
		);
	}

	@Test
	public void testGetIdUsingFieldResults() {
		doInHibernate( this::sessionFactory,
					   session -> {
						   List<User> users = session.createNamedQuery( "getIdUsingFieldResults", User.class ).getResultList();
						   Assertions.assertFalse( Hibernate.isPropertyInitialized( users.get(0), "principal" ) );
					   }
		);
	}

	@NamedNativeQuery(name = "getIdAndPrincipalUsingFieldResults", query = "select u.id as id, u.principal as principal from user_tbl u", resultSetMapping = "id_and_principal_with_fields")
	@NamedNativeQuery(name = "getIdUsingFieldResults", query = "select u.id as id from user_tbl u", resultSetMapping = "id_with_fields")
	@NamedNativeQuery(name = "getIdAndPrincipalWithoutUsingFieldResults", query = "select u.id as id, u.principal as principal from user_tbl u", resultSetMapping = "without_fields")
	@NamedNativeQuery(name = "getIdWithoutUsingFieldResults", query = "select u.id as id from user_tbl u", resultSetMapping = "without_fields")

	@SqlResultSetMapping( name = "id_and_principal_with_fields",
			entities = @EntityResult( entityClass = User.class, fields = { @FieldResult(name = "id", column = "id"), @FieldResult(name = "principal", column = "principal") } )
	)
	@SqlResultSetMapping( name = "id_with_fields",
			entities = @EntityResult( entityClass = User.class, fields = { @FieldResult(name = "id", column = "id") } )
	)
	@SqlResultSetMapping( name = "without_fields",
			entities = @EntityResult( entityClass = User.class )
	)

	@Entity( name = "User")
	@Table(name = "user_tbl")
	private static class User {
		@Id
		private Long id;
		@Basic(fetch = FetchType.LAZY)
		private Byte principal;

		public User() {
		}

		public User(Long id, Byte principal) {
			this.id = id;
			this.principal = principal;
		}
	}

}
