package org.hibernate.orm.test.bytecode.enhancement.cache;

import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue( jiraKey = "HHH-16473")
public class ManyToOneTestNoProxy extends BaseCoreFunctionalTestCase {

	private static final String ENTITY_A_NAME = "Alice";
	private static final String ENTITY_B_NAME = "Bob";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Actor.class,
				User.class,
				UserGroup.class,
				ActorGroup.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Before
	public void setUp() {
		inTransaction(
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
					
					session.persist(user1);
					session.persist(user2);
					session.persist(group1);
					session.persist(group2);
					session.persist(group3);
					session.persist(group4);
				}
		);

		inTransaction(
				session -> {
					session.getSessionFactory().getCache().evictAllRegions();
				}
		);
	}

	@Test
	public void testSelect() {
		inTransaction(session -> {
				User user = session.getReference(User.class, 1);

				assertThat( user ).isNotNull();
				assertThat( user.getName() ).isEqualTo( ENTITY_A_NAME );
			}
		);
	}
	
	@Entity
	@Table(name = "users")
	@Proxy(lazy = false)
	@BatchSize(size = 512)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cacheable(value = true)
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
	@Proxy(lazy = false)
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
	@Proxy(lazy = false)
	@DiscriminatorValue("USERS")
	@BatchSize(size = 256)
	public static class UserGroup extends ActorGroup<User> {
		
		public UserGroup() {
		}
	}
	
	@Entity
	@Proxy(lazy = false)
	@Table(name = "actor_group")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "TYPE")
	@Cacheable(value = true)
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
