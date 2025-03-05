/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cache;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
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
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@JiraKey("HHH-16473")
@DomainModel(
		annotatedClasses = {
			ManyToOneNoProxyTest.Actor.class,
				ManyToOneNoProxyTest.User.class,
				ManyToOneNoProxyTest.UserGroup.class,
				ManyToOneNoProxyTest.ActorGroup.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class ManyToOneNoProxyTest {

	private static final String ENTITY_A_NAME = "Alice";
	private static final String ENTITY_B_NAME = "Bob";


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					User user1 = new User();
					User user2 = new User();

					UserGroup group1 = new UserGroup();
					UserGroup group2 = new UserGroup();
					UserGroup group3 = new UserGroup();
					UserGroup group4 = new UserGroup();

					group1.parent = group2;
					group2.parent = group3;
					group3.parent = group4;

					user1.team = group1;
					user2.team = group1;

					user1.name = ENTITY_A_NAME;
					user2.name = ENTITY_B_NAME;

					session.persist( user1 );
					session.persist( user2 );
					session.persist( group1 );
					session.persist( group2 );
					session.persist( group3 );
					session.persist( group4 );
				}
		);

		scope.inTransaction(
				session -> {
					session.getSessionFactory().getCache().evictAllRegions();
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = session.getReference( User.class, 1 );

					assertThat( user ).isNotNull();
					assertThat( user.getName() ).isEqualTo( ENTITY_A_NAME );
				}
		);
	}

	@Entity
	@Table(name = "users")
	@BatchSize(size = 512)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "ACTOR")
	public static class Actor {
		Long id;

		String name;

		public Actor() {
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@BatchSize(size = 512)
	@DiscriminatorValue(value = "USER")
	public static class User extends Actor {
		UserGroup team;

		public User() {
			super();
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "team_id")
		public UserGroup getTeam() {
			return team;
		}

		public void setTeam(UserGroup team) {
			this.team = team;
		}
	}

	@Entity
	@DiscriminatorValue("USERS")
	@BatchSize(size = 256)
	public static class UserGroup extends ActorGroup<User> {

		public UserGroup() {
		}
	}

	@Entity
	@Table(name = "actor_group")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "TYPE")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@BatchSize(size = 256)
	public abstract static class ActorGroup<T extends Actor> {
		Long id;

		ActorGroup<T> parent;

		public ActorGroup() {
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY, targetEntity = ActorGroup.class)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "parent_id")
		public ActorGroup<T> getParent() {
			return parent;
		}

		public void setParent(ActorGroup<T> parent) {
			this.parent = parent;
		}

	}
}
