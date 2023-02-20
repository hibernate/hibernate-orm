package org.hibernate.orm.test.bytecode.enhancement.cache;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue( jiraKey = "HHH-16193")
public class ManyToOneTest extends BaseCoreFunctionalTestCase {

	private static final String ENTITY_B_NAME = "B1";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class,
				EntityB.class
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
					EntityB b1 = new EntityB( ENTITY_B_NAME );
					session.persist( b1 );
					EntityA a1 = new EntityA( "A1", b1 );
					session.persist( a1 );
				}
		);
	}

	@Test
	public void testSelect() {
		List<EntityA> entities = fromTransaction(
				session ->
						session.createNamedQuery( "PersonType.selectAll", EntityA.class )
								.getResultList()
		);

		assertThat( entities.size() ).isEqualTo( 1 );

		EntityA entityA = entities.get( 0 );
		EntityB entityB = entityA.getEntityB();

		assertThat( entities ).isNotNull();

		assertThat( entityB.getName() ).isEqualTo( ENTITY_B_NAME );
	}


	@Entity(name = "EntityA")
	@NamedQueries({
			@NamedQuery(name = "PersonType.selectAll", query = "SELECT a FROM EntityA a")
	})
	@BatchSize(size = 500)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(String name, EntityB entityB) {
			this.name = name;
			this.entityB = entityB;
		}

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

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB entityB) {
			this.entityB = entityB;
		}
	}

	@Entity(name = "EntityB")
	@BatchSize(size = 500)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class EntityB {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public EntityB() {
		}

		public EntityB(String name) {
			this.name = name;
		}

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

}
