package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.JavaType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-15874")
@DomainModel(
		annotatedClasses = {
				OverriddenGenericFieldAccessTest.GenericParentEntity.class,
				OverriddenGenericFieldAccessTest.WithGenericOverride.class,
				OverriddenGenericFieldAccessTest.ConcreteGenericSpecialization.class,
				OverriddenGenericFieldAccessTest.WithGenericOverrideAfterConcreteSpecialization.class,
				OverriddenGenericFieldAccessTest.ThreeLevelGenericMiddleEntity.class,
				OverriddenGenericFieldAccessTest.WithGenericMiddleOverride.class,
				OverriddenGenericFieldAccessTest.WithThreeLevelGenericOverride.class,
				OverriddenGenericFieldAccessTest.GenericMappedSuperclassMiddleEntity.class,
				OverriddenGenericFieldAccessTest.WithGenericMappedSuperclassOverride.class
		}
)
@SessionFactory
public class OverriddenGenericFieldAccessTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void childIsAccessedForOverriddenGenericFieldsWithGetters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithGenericOverride(
					"parent id",
					"child id",
					"parent property",
					"child property"
			);
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithGenericOverride.class, "child id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "child id" );
			assertThat( entity.getProperty() ).isEqualTo( "child property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithGenericOverride.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "parent_property" );
	}

	@Test
	void childIsAccessedForGenericFieldsOverriddenAfterConcreteSpecialization(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithGenericOverrideAfterConcreteSpecialization(
						"parent id",
						"child id",
						"parent property",
						"child property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithGenericOverrideAfterConcreteSpecialization.class, "child id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "child id" );
			assertThat( entity.getProperty() ).isEqualTo( "child property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithGenericOverrideAfterConcreteSpecialization.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "parent_property" );
	}

	@Test
	void childIsAccessedForGenericFieldsOverriddenAtThreeLevels(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithThreeLevelGenericOverride(
						"parent id",
						"middle id",
						"child id",
						"parent property",
						"middle property",
						"child property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithThreeLevelGenericOverride.class, "child id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "child id" );
			assertThat( entity.getProperty() ).isEqualTo( "child property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithThreeLevelGenericOverride.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "parent_property" );
	}

	@Test
	void middleIsAccessedForGenericFieldsNotOverriddenInConcreteChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithGenericMiddleOverride(
						"parent id",
						"middle id",
						"parent property",
						"middle property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithGenericMiddleOverride.class, "middle id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "middle id" );
			assertThat( entity.getProperty() ).isEqualTo( "middle property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithGenericMiddleOverride.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "parent_property" );
	}

	@Test
	void mappedSuperclassIsAccessedForOverriddenGenericFields(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithGenericMappedSuperclassOverride(
						"parent id",
						"mapped superclass id",
						"parent property",
						"mapped superclass property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find(
					WithGenericMappedSuperclassOverride.class,
					"mapped superclass id"
			);

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "mapped superclass id" );
			assertThat( entity.getProperty() ).isEqualTo( "mapped superclass property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithGenericMappedSuperclassOverride.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "parent_property" );
	}

	@Entity(name = "GenericParentEntity")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class GenericParentEntity<T> {
		@Id
		@JavaType(StringJavaType.class)
		private T id;

		@JavaType(StringJavaType.class)
		@Column(name = "parent_property")
		private T property;

		protected GenericParentEntity() {
		}

		protected GenericParentEntity(T id, T property) {
			this.id = id;
			this.property = property;
		}

		public T getId() {
			return id;
		}

		public T getProperty() {
			return property;
		}
	}

	@Entity(name = "WithGenericOverride")
	public static class WithGenericOverride extends GenericParentEntity<String> {
		private String id;

		@Column(name = "child_annotation_should_be_ignored")
		private String property;

		protected WithGenericOverride() {
		}

		public WithGenericOverride(
				String parentId,
				String childId,
				String parentProperty,
				String childProperty) {
			super( parentId, parentProperty );
			this.id = childId;
			this.property = childProperty;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "ThreeLevelGenericMiddleEntity")
	public abstract static class ThreeLevelGenericMiddleEntity<T> extends GenericParentEntity<T> {
		@JavaType(StringJavaType.class)
		private T id;

		@JavaType(StringJavaType.class)
		@Column(name = "middle_annotation_should_be_ignored")
		private T property;

		protected ThreeLevelGenericMiddleEntity() {
		}

		protected ThreeLevelGenericMiddleEntity(T parentId, T middleId, T parentProperty, T middleProperty) {
			super( parentId, parentProperty );
			this.id = middleId;
			this.property = middleProperty;
		}

		@Override
		public T getId() {
			return id;
		}

		@Override
		public T getProperty() {
			return property;
		}
	}

	@Entity(name = "ConcreteGenericSpecialization")
	public static class ConcreteGenericSpecialization extends GenericParentEntity<String> {
		protected ConcreteGenericSpecialization() {
		}

		protected ConcreteGenericSpecialization(String id, String property) {
			super( id, property );
		}
	}

	@Entity(name = "GenericOverrideAfterSpec")
	public static class WithGenericOverrideAfterConcreteSpecialization extends ConcreteGenericSpecialization {
		private String id;

		@Column(name = "child_annotation_should_be_ignored_after_specialization")
		private String property;

		protected WithGenericOverrideAfterConcreteSpecialization() {
		}

		public WithGenericOverrideAfterConcreteSpecialization(
				String parentId,
				String childId,
				String parentProperty,
				String childProperty) {
			super( parentId, parentProperty );
			this.id = childId;
			this.property = childProperty;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "WithGenericMiddleOverride")
	public static class WithGenericMiddleOverride extends ThreeLevelGenericMiddleEntity<String> {
		protected WithGenericMiddleOverride() {
		}

		public WithGenericMiddleOverride(
				String parentId,
				String middleId,
				String parentProperty,
				String middleProperty) {
			super( parentId, middleId, parentProperty, middleProperty );
		}
	}

	@Entity(name = "WithThreeLevelGenericOverride")
	public static class WithThreeLevelGenericOverride extends ThreeLevelGenericMiddleEntity<String> {
		private String id;

		@Column(name = "child_annotation_should_be_ignored")
		private String property;

		protected WithThreeLevelGenericOverride() {
		}

		public WithThreeLevelGenericOverride(
				String parentId,
				String middleId,
				String childId,
				String parentProperty,
				String middleProperty,
				String childProperty) {
			super( parentId, middleId, parentProperty, middleProperty );
			this.id = childId;
			this.property = childProperty;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getProperty() {
			return property;
		}
	}

	@MappedSuperclass
	public abstract static class GenericMappedSuperclassMiddleEntity<T> extends GenericParentEntity<T> {
		private T id;

		@Column(name = "mapped_superclass_annotation_should_be_ignored")
		private T property;

		protected GenericMappedSuperclassMiddleEntity() {
		}

		protected GenericMappedSuperclassMiddleEntity(
				T parentId,
				T mappedSuperclassId,
				T parentProperty,
				T mappedSuperclassProperty) {
			super( parentId, parentProperty );
			this.id = mappedSuperclassId;
			this.property = mappedSuperclassProperty;
		}

		@Override
		public T getId() {
			return id;
		}

		@Override
		public T getProperty() {
			return property;
		}
	}

	@Entity(name = "GenericMappedChild")
	public static class WithGenericMappedSuperclassOverride
			extends GenericMappedSuperclassMiddleEntity<String> {
		protected WithGenericMappedSuperclassOverride() {
		}

		public WithGenericMappedSuperclassOverride(
				String parentId,
				String mappedSuperclassId,
				String parentProperty,
				String mappedSuperclassProperty) {
			super( parentId, mappedSuperclassId, parentProperty, mappedSuperclassProperty );
		}
	}
}
