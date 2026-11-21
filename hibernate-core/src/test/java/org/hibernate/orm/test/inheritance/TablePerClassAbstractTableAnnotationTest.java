package org.hibernate.orm.test.inheritance;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-21202")
@ServiceRegistry
public class TablePerClassAbstractTableAnnotationTest {

	private static final String MESSAGE =
			"is an abstract class with subclasses in a 'TABLE_PER_CLASS' hierarchy and may not be annotated '@Table'";

	@Test
	void abstractRoot(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( AnnotatedRoot.class, AnnotatedRootChild.class ) ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( AnnotatedRoot.class.getName() )
				.hasMessageContaining( MESSAGE );
	}

	@Test
	void abstractIntermediate(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( Root.class, AnnotatedIntermediate.class, AnnotatedIntermediateChild.class ) ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( AnnotatedIntermediate.class.getName() )
				.hasMessageContaining( MESSAGE );
	}

	@Test
	void abstractRootWithoutSubclasses(ServiceRegistryScope registryScope) {
		final Metadata metadata = MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( AnnotatedRootWithoutSubclasses.class ) );
		assertThat( metadata.getEntityBinding( AnnotatedRootWithoutSubclasses.class.getName() )
				.getTable().getName() )
				.isEqualTo( "lonely_t" );
	}

	@Test
	void indexOnConcreteSubclass(ServiceRegistryScope registryScope) {
		final Metadata metadata = MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( Root.class, Child.class ) );
		final var table = metadata.getEntityBinding( Child.class.getName() ).getTable();
		assertThat( table.getName() ).isEqualTo( "child_t" );
		assertThat( table.getIndexes() ).containsKey( "idx_child_name" );
		assertThat( table.getIndexes().get( "idx_child_name" ).getSelectables() )
				.singleElement()
				.satisfies( column -> assertThat( column.getText() ).isEqualTo( "name" ) );
	}

	@Entity(name = "AnnotatedRoot")
	@Inheritance(strategy = TABLE_PER_CLASS)
	@Table(name = "root_t", indexes = @Index(name = "idx_root_name", columnList = "name"))
	static abstract class AnnotatedRoot {
		@Id
		long id;
		String name;
	}

	@Entity(name = "AnnotatedRootChild")
	@Table(name = "annotated_root_child_t")
	static class AnnotatedRootChild extends AnnotatedRoot {
	}

	@Entity(name = "Root")
	@Inheritance(strategy = TABLE_PER_CLASS)
	static abstract class Root {
		@Id
		long id;
		String name;
	}

	@Entity(name = "AnnotatedIntermediate")
	@Table(name = "intermediate_t")
	static abstract class AnnotatedIntermediate extends Root {
	}

	@Entity(name = "AnnotatedIntermediateChild")
	static class AnnotatedIntermediateChild extends AnnotatedIntermediate {
	}

	@Entity(name = "Child")
	@Table(name = "child_t", indexes = @Index(name = "idx_child_name", columnList = "name"))
	static class Child extends Root {
	}

	@Entity(name = "AnnotatedRootWithoutSubclasses")
	@Inheritance(strategy = TABLE_PER_CLASS)
	@Table(name = "lonely_t")
	static abstract class AnnotatedRootWithoutSubclasses {
		@Id
		long id;
	}
}
