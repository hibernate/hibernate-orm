/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.batch;

import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.NoDirtyCheckEnhancementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class })
public abstract class AbstractBatchingTest extends BaseNonConfigCoreFunctionalTestCase {
	protected String childName = UUID.randomUUID().toString();
	protected Long parentId;

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "100" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClass( PaddedBatchingTest.ParentEntity.class );
		sources.addAnnotatedClass( PaddedBatchingTest.ChildEntity.class );
	}

	@Test
	public void testLoadParent() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		inTransaction(
				session -> {
					ParentEntity parentEntity = session.find( ParentEntity.class, parentId );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					ChildEntity childEntity = parentEntity.getChildEntity();

					assertFalse( Hibernate.isPropertyInitialized( childEntity, "name" ) );
					assertEquals( childName, childEntity.getName() );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Before
	public void setUp() {
		PaddedBatchingTest.ParentEntity parent = new PaddedBatchingTest.ParentEntity();
		inTransaction(
				session -> {
					PaddedBatchingTest.ChildEntity childEntity = new PaddedBatchingTest.ChildEntity();

					childEntity.setName( childName );

					parent.setChildEntity( childEntity );
					session.persist( parent );
				}
		);
		parentId = parent.getId();
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		private Long id;

		private ChildEntity childEntity;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@JoinColumn(name = "FK_CHILD_ENTITY_ID", nullable = false)
		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		public ChildEntity getChildEntity() {
			return childEntity;
		}

		public void setChildEntity(ChildEntity childEntity) {
			this.childEntity = childEntity;
		}
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		private Long id;

		private String name;

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
}
