/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-10183")
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 10)
@DomainModel(
		annotatedClasses = SQLServer2008NationalizedScalarQueryTest.User.class
)
@SessionFactory
public class SQLServer2008NationalizedScalarQueryTest {


	@Test
	public void testScalarResult(SessionFactoryScope scope) {

		User user1 = new User( 1, "Chris" );
		User user2 = new User( 2, "Steve" );

		scope.inTransaction( session -> {
			session.save( user1 );
			session.save( user2 );
		} );

		scope.inTransaction( session -> {
			List<Object[]> users = session.createNativeQuery(
					"select * from users" ).getResultList();
			assertEquals( 2, users.size() );
		} );
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		private Integer id;
		private String name;

		public User() {

		}

		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Nationalized
		@Column(nullable = false)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
}
