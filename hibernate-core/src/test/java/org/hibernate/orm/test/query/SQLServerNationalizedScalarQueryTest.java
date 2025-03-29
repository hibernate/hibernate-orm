/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.JiraKey;
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
@RequiresDialect(value = SQLServerDialect.class)
@DomainModel(
		annotatedClasses = SQLServerNationalizedScalarQueryTest.User.class
)
@SessionFactory
public class SQLServerNationalizedScalarQueryTest {

	@JiraKey(value = "HHH-16857")
	@Test
	public void testLiteral(SessionFactoryScope scope) {
		scope.inTransaction(session -> session.createSelectionQuery("from User where name = 'Gavin'").getResultList());
		scope.inTransaction(session -> session.createSelectionQuery("from User where role = 'ADMIN'").getResultList());
	}

	@JiraKey(value = "HHH-10183")
	@Test
	public void testScalarResult(SessionFactoryScope scope) {

		User user1 = new User( 1, "Chris" );
		User user2 = new User( 2, "Steve" );

		scope.inTransaction( session -> {
			session.persist( user1 );
			session.persist( user2 );
		} );

		scope.inTransaction( session -> {
			List<Object[]> users = session.createNativeQuery("select * from users" ).getResultList();
			assertEquals( 2, users.size() );
		} );
	}

	enum Role { ADMIN, USER, GUEST }

	static class Converter implements AttributeConverter<Role,String> {
		@Override
		public String convertToDatabaseColumn(Role attribute) {
			return attribute==null ? null : attribute.name();
		}

		@Override
		public Role convertToEntityAttribute(String name) {
			return name==null ? null : Role.valueOf(name);
		}
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		private Integer id;
		private String name;
		private Role role = Role.USER;

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

		@Nationalized
		@Convert(converter = Converter.class)
		public Role getRole() {
			return role;
		}

		public void setRole(Role role) {
			this.role = role;
		}
	}
}
