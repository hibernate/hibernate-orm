/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.callbacks;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.jpa.boot.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityListener;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PreUpsert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.buildCategorizedDomainModel;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class CallbackTests {
	@Test
	@ServiceRegistry
	void testMappedSuper(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();

					final MappedSuperclass mappedSuper = metadataCollector.getMappedSuperclass( HierarchySuper.class );
					assertThat( mappedSuper ).isNotNull();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( HierarchyRoot.class.getName() );
					assertThat( entityBinding ).isNotNull();
					assertThat( entityBinding.getSuperPersistentClass() ).isNull();
					assertThat( entityBinding.getSuperType() ).isEqualTo( mappedSuper );
					assertThat( entityBinding.getSuperMappedSuperclass() ).isEqualTo( mappedSuper );
					assertThat( entityBinding.getCallbackDefinitions() ).hasSize( 3 );
				},
				scope.getRegistry(),
				HierarchySuper.class,
				HierarchyRoot.class
		);
	}

	@Test
	@ServiceRegistry
	void testJpa4LifecycleCallbacks(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( Jpa4LifecycleEntity.class.getName() );
					assertThat( entityBinding.getCallbackDefinitions() )
							.extracting( CallbackTests::callbackType )
							.containsExactly(
									CallbackType.PRE_INSERT,
									CallbackType.POST_INSERT,
									CallbackType.PRE_UPSERT,
									CallbackType.POST_UPSERT,
									CallbackType.PRE_DELETE,
									CallbackType.POST_DELETE,
									CallbackType.PRE_MERGE
							);
				},
				scope.getRegistry(),
				Jpa4LifecycleEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testLifecycleCallbacksReachRuntime(ServiceRegistryScope scope) {
		RuntimeCallbackListener.prePersistCount = 0;
		RuntimeCallbackEntity.postLoadCount = 0;

		checkDomainModel(
				(context) -> {
					try (var sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( context.getMetadata() )) {
						sessionFactory.getSchemaManager().create( true );
						try {
							try (var session = sessionFactory.openSession()) {
								final var transaction = session.beginTransaction();
								session.persist( new RuntimeCallbackEntity( 1, "one" ) );
								assertThat( RuntimeCallbackListener.prePersistCount ).isEqualTo( 1 );
								transaction.commit();
							}

							try (var session = sessionFactory.openSession()) {
								final var transaction = session.beginTransaction();
								assertThat( session.find( RuntimeCallbackEntity.class, 1 ) ).isNotNull();
								assertThat( RuntimeCallbackEntity.postLoadCount ).isEqualTo( 1 );
								transaction.commit();
							}
						}
						finally {
							sessionFactory.getSchemaManager().drop( true );
						}
					}
				},
				scope.getRegistry(),
				RuntimeCallbackEntity.class
		);
	}

	@Test
	void testSimpleEventListenerResolution() {
		final CategorizedDomainModel categorizedDomainModel = buildCategorizedDomainModel(
				HierarchyRoot.class,
				HierarchySuper.class
		);
		final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
		final EntityHierarchy hierarchy = entityHierarchies.iterator().next();

		final EntityTypeMetadata rootMapping = hierarchy.getRoot();
		assertThat( rootMapping.getHierarchyJpaEventListeners() ).hasSize( 3 );
		final List<String> listenerClassNames = rootMapping.getHierarchyJpaEventListeners()
				.stream()
				.map( listener -> listener.getCallbackClass().getClassName() )
				.collect( Collectors.toList() );
		assertThat( listenerClassNames ).containsExactly(
				Listener1.class.getName(),
				Listener2.class.getName(),
				HierarchyRoot.class.getName()
		);

		final IdentifiableTypeMetadata superMapping = rootMapping.getSuperType();
		assertThat( superMapping.getHierarchyJpaEventListeners() ).hasSize( 1 );
		assertThat( superMapping.getHierarchyJpaEventListeners().get( 0 ).getCallbackClass()
				.getDirectAnnotationUsage( EntityListener.class ) )
				.isNotNull();
		final String callbackClassName = superMapping.getHierarchyJpaEventListeners()
				.get( 0 )
				.getCallbackClass()
				.getClassName();
		assertThat( callbackClassName ).isEqualTo( Listener1.class.getName() );
	}

	private static CallbackType callbackType(CallbackDefinition callbackDefinition) {
		try {
			final Field callbackTypeField = callbackDefinition.getClass().getDeclaredField( "callbackType" );
			callbackTypeField.setAccessible( true );
			return (CallbackType) callbackTypeField.get( callbackDefinition );
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "Jpa4LifecycleEntity")
	public static class Jpa4LifecycleEntity {
		@Id
		private Integer id;

		@PreInsert
		public void preInsert() {
		}

		@PostInsert
		public void postInsert() {
		}

		@PreUpsert
		public void preUpsert() {
		}

		@PostUpsert
		public void postUpsert() {
		}

		@PreDelete
		public void preDelete() {
		}

		@PostDelete
		public void postDelete() {
		}

		@PreMerge
		public void preMerge() {
		}
	}

	@Entity(name = "RuntimeCallbackEntity")
	@EntityListeners(RuntimeCallbackListener.class)
	public static class RuntimeCallbackEntity {
		private static int postLoadCount;

		@Id
		private Integer id;
		private String name;

		protected RuntimeCallbackEntity() {
			// for Hibernate use
		}

		public RuntimeCallbackEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@PostLoad
		public void postLoad() {
			postLoadCount++;
		}
	}

	public static class RuntimeCallbackListener {
		private static int prePersistCount;

		@PrePersist
		public void prePersist(RuntimeCallbackEntity entity) {
			prePersistCount++;
		}
	}
}
