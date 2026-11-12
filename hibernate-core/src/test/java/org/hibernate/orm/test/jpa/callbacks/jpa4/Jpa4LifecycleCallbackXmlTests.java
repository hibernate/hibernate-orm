package org.hibernate.orm.test.jpa.callbacks.jpa4;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Same tests as {@link Jpa4LifecycleCallbackAnnotationTests}, with an unannotated model whose
 * entities, mapped superclass and entity listener are mapped in {@code orm.xml}.
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/jpa/callbacks/jpa4/jpa4-lifecycle-callbacks.orm.xml")
@SessionFactory
public class Jpa4LifecycleCallbackXmlTests extends AbstractJpa4LifecycleCallbackTests {

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

	public static class LifecycleEntity implements NamedEntity {
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

		void prePersist() {
			Events.names.add( "pre-persist:" + name );
		}

		void postPersist() {
			Events.names.add( "post-persist:" + name );
		}

		void preMerge() {
			Events.names.add( "pre-merge:" + name );
			name = name + "-pre-merge";
		}

		void preInsert() {
			Events.names.add( "pre-insert:" + name );
		}

		void postInsert() {
			Events.names.add( "post-insert:" + name );
		}

		void preUpsert() {
			Events.names.add( "pre-upsert:" + name );
		}

		void postUpsert() {
			Events.names.add( "post-upsert:" + name );
		}

		void preRemove() {
			Events.names.add( "pre-remove:" + name );
		}

		void postRemove() {
			Events.names.add( "post-remove:" + name );
		}

		void preDelete() {
			Events.names.add( "pre-delete:" + name );
		}

		void postDelete() {
			Events.names.add( "post-delete:" + name );
		}
	}

	public static class MutatingEntity implements NamedEntity {
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

		void preInsert() {
			Events.names.add( "mutating-pre-insert:" + name );
			name = name + "-pre-insert";
		}

		void preUpsert() {
			Events.names.add( "mutating-pre-upsert:" + name );
			name = name + "-pre-upsert";
		}
	}

	public static class PlainEntity implements NamedEntity {
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

	public static class ListenedEntity implements NamedEntity {
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
		void prePersist(Object entity) {
			Events.record( "listener-pre-persist", entity );
		}

		void postPersist(Object entity) {
			Events.record( "listener-post-persist", entity );
		}

		void preMerge(Object entity) {
			Events.record( "listener-pre-merge", entity );
		}

		void preInsert(Object entity) {
			Events.record( "listener-pre-insert", entity );
		}

		void postInsert(Object entity) {
			Events.record( "listener-post-insert", entity );
		}

		void preUpsert(Object entity) {
			Events.record( "listener-pre-upsert", entity );
		}

		void postUpsert(Object entity) {
			Events.record( "listener-post-upsert", entity );
		}

		void preRemove(Object entity) {
			Events.record( "listener-pre-remove", entity );
		}

		void postRemove(Object entity) {
			Events.record( "listener-post-remove", entity );
		}

		void preDelete(Object entity) {
			Events.record( "listener-pre-delete", entity );
		}

		void postDelete(Object entity) {
			Events.record( "listener-post-delete", entity );
		}
	}

	public static class LifecycleSuperclass implements NamedEntity {
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

		void prePersist() {
			Events.record( "superclass-pre-persist", this );
		}

		void postPersist() {
			Events.record( "superclass-post-persist", this );
		}

		void preMerge() {
			Events.record( "superclass-pre-merge", this );
		}

		void preInsert() {
			Events.record( "superclass-pre-insert", this );
		}

		void postInsert() {
			Events.record( "superclass-post-insert", this );
		}

		void preUpsert() {
			Events.record( "superclass-pre-upsert", this );
		}

		void postUpsert() {
			Events.record( "superclass-post-upsert", this );
		}

		void preRemove() {
			Events.record( "superclass-pre-remove", this );
		}

		void postRemove() {
			Events.record( "superclass-post-remove", this );
		}

		void preDelete() {
			Events.record( "superclass-pre-delete", this );
		}

		void postDelete() {
			Events.record( "superclass-post-delete", this );
		}
	}

	public static class SuperclassEntity extends LifecycleSuperclass {
		public SuperclassEntity() {
		}

		public SuperclassEntity(Integer id, String name) {
			super( id, name );
		}
	}
}
