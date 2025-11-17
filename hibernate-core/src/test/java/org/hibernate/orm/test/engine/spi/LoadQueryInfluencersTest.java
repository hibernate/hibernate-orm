/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		LoadQueryInfluencersTest.EntityWithBatchSize1.class,
		LoadQueryInfluencersTest.ChildEntity.class
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "100")
		}
)
public class LoadQueryInfluencersTest {

	@Test
	public void usesCustomEntityBatchSizeForEffectivelyBatchLoadable(SessionFactoryScope scope) {
		scope.inSession(
				(nonTransactedSession) -> {
					LoadQueryInfluencers influencers = new LoadQueryInfluencers( nonTransactedSession.getSessionFactory() );
					assertTrue( influencers.getBatchSize() > 1, "Expecting default batch size > 1" );

					EntityPersister persister = nonTransactedSession.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( EntityWithBatchSize1.class );

					assertFalse( persister.isBatchLoadable(), "Incorrect persister batch loadable." );
					assertEquals( 1, influencers.effectiveBatchSize( persister ), "Incorrect effective batch size." );
					assertFalse(
							influencers.effectivelyBatchLoadable( persister ),
							"Incorrect effective batch loadable."
					);
				}
		);
	}

	@Test
	public void usesCustomCollectionBatchSizeForEffectivelyBatchLoadable(SessionFactoryScope scope) {
		scope.inSession(
				(nonTransactedSession) -> {
					LoadQueryInfluencers influencers = new LoadQueryInfluencers( nonTransactedSession.getSessionFactory() );
					assertTrue( influencers.getBatchSize() > 1, "Expecting default batch size > 1" );

					EntityPersister entityPersister = nonTransactedSession.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( EntityWithBatchSize1.class );

					NavigableRole collectionRole = entityPersister.getNavigableRole()
							.append( "childrenWithBatchSize1" );

					CollectionPersister persister = nonTransactedSession.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.findCollectionDescriptor( collectionRole.getFullPath() );

					assertFalse( persister.isBatchLoadable(), "Incorrect persister batch loadable." );
					assertEquals( 1, influencers.effectiveBatchSize( persister ), "Incorrect effective batch size." );
					assertFalse(
							influencers.effectivelyBatchLoadable( persister ),
							"Incorrect effective batch loadable."
					);
				}
		);
	}


	@Entity
	@Table(name = "EntityWithBatchSize1")
	@BatchSize(size = 1)
	public class EntityWithBatchSize1 {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@OneToMany
		@BatchSize(size = 1)
		private Set<ChildEntity> childrenWithBatchSize1;

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

		public Set<ChildEntity> getChildrenWithBatchSize1() {
			return childrenWithBatchSize1;
		}

		public void setChildrenWithBatchSize1(Set<ChildEntity> childrenWithBatchSize1) {
			this.childrenWithBatchSize1 = childrenWithBatchSize1;
		}
	}

	@Entity
	@Table(name = "ChildEntity")
	public class ChildEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

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
