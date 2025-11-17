/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.associations;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		OneToManyEagerDiscriminatorTest.User.class,
		OneToManyEagerDiscriminatorTest.ValueBase.class,
		OneToManyEagerDiscriminatorTest.UserValueBase.class
})
@JiraKey("HHH-15829")
public class OneToManyEagerDiscriminatorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = new User();
			final UserValueBase value = new UserValueBase();
			value.setData( "value_1" );
			value.setEntity( user );
			session.persist( user );
			session.persist( value );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ValueBase" ).executeUpdate() );
		scope.inTransaction( session -> session.createMutationQuery( "delete from User" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = session.find( User.class, 1L );
			assertNotNull( user );
			assertTrue( Hibernate.isInitialized( user.getProperties() ) );
			assertEquals( 1, user.getProperties().size() );
			assertEquals( "value_1", user.getProperties().iterator().next().getData() );
		} );
	}

	@Entity(name = "User")
	@Table(name = "Users")
	public static class User implements Serializable {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "entity", fetch = FetchType.EAGER)
		private Set<UserValueBase> properties = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<UserValueBase> getProperties() {
			return properties;
		}

		public void setProperties(Set<UserValueBase> properties) {
			this.properties = properties;
		}
	}

	@Entity(name = "ValueBase")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "target_type", discriminatorType = DiscriminatorType.INTEGER)
	public abstract static class ValueBase implements Serializable {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "UserValueBase")
	@DiscriminatorValue("1")
	public static class UserValueBase extends ValueBase {
		private String data;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "entity_id", nullable = false)
		private User entity;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public User getEntity() {
			return entity;
		}

		public void setEntity(User entity) {
			this.entity = entity;
		}
	}
}
