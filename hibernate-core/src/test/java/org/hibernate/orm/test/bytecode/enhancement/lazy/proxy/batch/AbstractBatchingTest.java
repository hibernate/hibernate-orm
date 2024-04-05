/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.batch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.NoDirtyCheckEnhancementContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				AbstractBatchingTest.ParentEntity.class, AbstractBatchingTest.ChildEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class })
public abstract class AbstractBatchingTest {
	protected String childName = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
	protected Long parentId;

	@Test
	public void testLoadParent(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					ParentEntity parentEntity = session.find( ParentEntity.class, parentId );

					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1L );

					ChildEntity childEntity = parentEntity.getChildEntity();

					assertFalse( Hibernate.isPropertyInitialized( childEntity, "name" ) );
					assertEquals( childName, childEntity.getName() );

					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 2L );
				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		ParentEntity parent = new ParentEntity();
		scope.inTransaction(
				session -> {
					ChildEntity childEntity = new ChildEntity();

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
