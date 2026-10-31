package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpsert;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		Jpa4LifecycleCallbackAnnotationTests.LifecycleEntity.class,
		Jpa4LifecycleCallbackAnnotationTests.MutatingEntity.class,
		Jpa4LifecycleCallbackAnnotationTests.PlainEntity.class,
		Jpa4LifecycleCallbackAnnotationTests.TypedListenerEntity.class,
		Jpa4LifecycleCallbackAnnotationTests.ListenedEntity.class,
		Jpa4LifecycleCallbackAnnotationTests.SuperclassEntity.class
})
@SessionFactory
public class Jpa4LifecycleCallbackAnnotationTests extends AbstractJpa4LifecycleCallbackTests {

	@Override
	protected NamedEntity newLifecycleEntity(Integer id, String name) {
		return new LifecycleEntity( id, name );
	}

	@Override
	protected Class<? extends NamedEntity> lifecycleEntityClass() {
		return LifecycleEntity.class;
	}

	@Override
	protected NamedEntity newMutatingEntity(Integer id, String name) {
		return new MutatingEntity( id, name );
	}

	@Override
	protected Class<? extends NamedEntity> mutatingEntityClass() {
		return MutatingEntity.class;
	}

	@Override
	protected NamedEntity newPlainEntity(Integer id, String name) {
		return new PlainEntity( id, name );
	}

	@Override
	protected NamedEntity newListenedEntity(Integer id, String name) {
		return new ListenedEntity( id, name );
	}

	@Override
	protected Class<? extends NamedEntity> listenedEntityClass() {
		return ListenedEntity.class;
	}

	@Override
	protected NamedEntity newSuperclassEntity(Integer id, String name) {
		return new SuperclassEntity( id, name );
	}

	@Override
	protected Class<? extends NamedEntity> superclassEntityClass() {
		return SuperclassEntity.class;
	}

	@Test
	void entityListenerCanHaveMultipleCallbacksForSameEventWhenParameterTypesDiffer(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TypedListenerEntity( 1, "typed" ) ) );

		assertThat( Events.names ).containsExactlyInAnyOrder(
				"listener-object:TypedListenerEntity",
				"listener-typed:typed"
		);
	}

	@Entity(name = "Jpa4LifecycleEntity")
	public static class LifecycleEntity implements NamedEntity {
		@Id
		private Integer id;
		private String name;

		public LifecycleEntity() {
		}

		public LifecycleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@PrePersist
		void prePersist() {
			Events.names.add( "pre-persist:" + name );
		}

		@PostPersist
		void postPersist() {
			Events.names.add( "post-persist:" + name );
		}

		@PreMerge
		void preMerge() {
			Events.names.add( "pre-merge:" + name );
			name = name + "-pre-merge";
		}

		@PreInsert
		void preInsert() {
			Events.names.add( "pre-insert:" + name );
		}

		@PostInsert
		void postInsert() {
			Events.names.add( "post-insert:" + name );
		}

		@PreUpsert
		void preUpsert() {
			Events.names.add( "pre-upsert:" + name );
		}

		@PostUpsert
		void postUpsert() {
			Events.names.add( "post-upsert:" + name );
		}

		@PreRemove
		void preRemove() {
			Events.names.add( "pre-remove:" + name );
		}

		@PostRemove
		void postRemove() {
			Events.names.add( "post-remove:" + name );
		}

		@PreDelete
		void preDelete() {
			Events.names.add( "pre-delete:" + name );
		}

		@PostDelete
		void postDelete() {
			Events.names.add( "post-delete:" + name );
		}
	}

	@Entity(name = "Jpa4LifecycleMutatingEntity")
	public static class MutatingEntity implements NamedEntity {
		@Id
		private Integer id;
		private String name;

		public MutatingEntity() {
		}

		public MutatingEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@PreInsert
		void preInsert() {
			Events.names.add( "mutating-pre-insert:" + name );
			name = name + "-pre-insert";
		}

		@PreUpsert
		void preUpsert() {
			Events.names.add( "mutating-pre-upsert:" + name );
			name = name + "-pre-upsert";
		}
	}

	@Entity(name = "Jpa4LifecyclePlainEntity")
	public static class PlainEntity implements NamedEntity {
		@Id
		private Integer id;
		private String name;

		public PlainEntity() {
		}

		public PlainEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Jpa4LifecycleTypedListenerEntity")
	@EntityListeners( MultiTypeListener.class )
	public static class TypedListenerEntity {
		@Id
		private Integer id;
		private String name;

		public TypedListenerEntity() {
		}

		public TypedListenerEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class MultiTypeListener {
		@PostPersist
		void any(Object entity) {
			Events.names.add( "listener-object:" + entity.getClass().getSimpleName() );
		}

		@PostPersist
		void typed(TypedListenerEntity entity) {
			Events.names.add( "listener-typed:" + entity.name );
		}

		@PostPersist
		void plain(PlainEntity entity) {
			Events.names.add( "listener-plain:" + entity.name );
		}
	}

	@Entity(name = "Jpa4LifecycleListenedEntity")
	@EntityListeners( LifecycleListener.class )
	public static class ListenedEntity implements NamedEntity {
		@Id
		private Integer id;
		private String name;

		public ListenedEntity() {
		}

		public ListenedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}

	public static class LifecycleListener {
		@PrePersist
		void prePersist(Object entity) {
			Events.record( "listener-pre-persist", entity );
		}

		@PostPersist
		void postPersist(Object entity) {
			Events.record( "listener-post-persist", entity );
		}

		@PreMerge
		void preMerge(Object entity) {
			Events.record( "listener-pre-merge", entity );
		}

		@PreInsert
		void preInsert(Object entity) {
			Events.record( "listener-pre-insert", entity );
		}

		@PostInsert
		void postInsert(Object entity) {
			Events.record( "listener-post-insert", entity );
		}

		@PreUpsert
		void preUpsert(Object entity) {
			Events.record( "listener-pre-upsert", entity );
		}

		@PostUpsert
		void postUpsert(Object entity) {
			Events.record( "listener-post-upsert", entity );
		}

		@PreRemove
		void preRemove(Object entity) {
			Events.record( "listener-pre-remove", entity );
		}

		@PostRemove
		void postRemove(Object entity) {
			Events.record( "listener-post-remove", entity );
		}

		@PreDelete
		void preDelete(Object entity) {
			Events.record( "listener-pre-delete", entity );
		}

		@PostDelete
		void postDelete(Object entity) {
			Events.record( "listener-post-delete", entity );
		}
	}

	@MappedSuperclass
	public static class LifecycleSuperclass implements NamedEntity {
		@Id
		private Integer id;
		private String name;

		public LifecycleSuperclass() {
		}

		public LifecycleSuperclass(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@PrePersist
		void prePersist() {
			Events.record( "superclass-pre-persist", this );
		}

		@PostPersist
		void postPersist() {
			Events.record( "superclass-post-persist", this );
		}

		@PreMerge
		void preMerge() {
			Events.record( "superclass-pre-merge", this );
		}

		@PreInsert
		void preInsert() {
			Events.record( "superclass-pre-insert", this );
		}

		@PostInsert
		void postInsert() {
			Events.record( "superclass-post-insert", this );
		}

		@PreUpsert
		void preUpsert() {
			Events.record( "superclass-pre-upsert", this );
		}

		@PostUpsert
		void postUpsert() {
			Events.record( "superclass-post-upsert", this );
		}

		@PreRemove
		void preRemove() {
			Events.record( "superclass-pre-remove", this );
		}

		@PostRemove
		void postRemove() {
			Events.record( "superclass-post-remove", this );
		}

		@PreDelete
		void preDelete() {
			Events.record( "superclass-pre-delete", this );
		}

		@PostDelete
		void postDelete() {
			Events.record( "superclass-post-delete", this );
		}
	}

	@Entity(name = "Jpa4LifecycleSuperclassEntity")
	public static class SuperclassEntity extends LifecycleSuperclass {
		public SuperclassEntity() {
		}

		public SuperclassEntity(Integer id, String name) {
			super( id, name );
		}
	}
}
