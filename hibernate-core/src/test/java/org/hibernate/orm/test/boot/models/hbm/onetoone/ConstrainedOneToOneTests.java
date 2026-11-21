package org.hibernate.orm.test.boot.models.hbm.onetoone;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.XmlMappingSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import jakarta.xml.bind.JAXBException;

import org.hibernate.Hibernate;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/// Verifies shared-primary-key and property-ref joins in mapping XML, including a JAXB round trip.
///
/// @author Steve Ebersole

@JiraKey("HHH-21112")
@ServiceRegistry
@SuppressWarnings("JUnitMalformedDeclaration")
public class ConstrainedOneToOneTests {
	private static final String MAPPING = "mappings/models/hbm/onetoone/constrained.orm.xml";
	private static final String PROPERTY_REF_MAPPING = "mappings/models/hbm/onetoone/property-ref.orm.xml";

	@Test
	@DomainModel(xmlMappings = MAPPING)
	void testMapping(DomainModelScope scope) {
		verifySharedPrimaryKey( scope.getDomainModel() );
	}

	@Test
	void testSerializedMapping(ServiceRegistryScope scope) throws JAXBException {
		verifySharedPrimaryKey( roundTrip( MAPPING, scope ) );
	}

	@Test
	@DomainModel(xmlMappings = PROPERTY_REF_MAPPING)
	void testPropertyRefMapping(DomainModelScope scope) {
		verifyPropertyRefConstraints( scope.getDomainModel() );
	}

	@Test
	void testPropertyRefSerializedMapping(ServiceRegistryScope scope) throws JAXBException {
		verifyPropertyRefConstraints( roundTrip( PROPERTY_REF_MAPPING, scope ) );
	}

	@Test
	@DomainModel(xmlMappings = PROPERTY_REF_MAPPING)
	@SessionFactory
	void testPropertyRefJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new CodedParent();
			parent.id = 1L;
			parent.code = 100L;
			session.persist( parent );
			// its identifier, but not its code, matches the children
			final var other = new CodedParent();
			other.id = 100L;
			other.code = 999L;
			session.persist( other );
			final var constrainedChild = new ConstrainedRefChild();
			constrainedChild.id = 100L;
			constrainedChild.parent = parent;
			session.persist( constrainedChild );
			final var child = new RefChild();
			child.id = 100L;
			child.parent = parent;
			session.persist( child );
			final var orphan = new RefChild();
			orphan.id = 200L;
			session.persist( orphan );
		} );
		scope.inTransaction( session -> {
			final var constrainedChild = session.find( ConstrainedRefChild.class, 100L );
			assertThat( Hibernate.unproxy( constrainedChild.parent, CodedParent.class ).id ).isEqualTo( 1L );
			final var child = session.find( RefChild.class, 100L );
			assertThat( Hibernate.unproxy( child.parent, CodedParent.class ).id ).isEqualTo( 1L );
			assertThat( session.find( RefChild.class, 200L ).parent ).isNull();
		} );
	}

	private static void verifySharedPrimaryKey(Metadata metadata) {
		final var child = metadata.getEntityBinding( Child.class.getName() );
		final var parent = child.getProperty( "parent" );
		assertThat( parent.getValue() ).isInstanceOf( OneToOne.class );
		assertThat( ( (OneToOne) parent.getValue() ).isConstrained() ).isTrue();

		final var table = child.getTable();
		assertThat( table.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
		assertThat( table.getForeignKeyCollection() ).hasSize( 1 );
		final var foreignKey = table.getForeignKeyCollection().iterator().next();
		assertThat( foreignKey.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
		assertThat( foreignKey.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
		assertThat( foreignKey.getOnDeleteAction() ).isEqualTo( OnDeleteAction.CASCADE );
	}

	private static void verifyPropertyRef(Metadata metadata) {
		assertSoftly( softly -> {
			for ( var child : List.of( ConstrainedRefChild.class, RefChild.class ) ) {
				final var binding = metadata.getEntityBinding( child.getName() );
				softly.assertThat( ( (ToOne) binding.getProperty( "parent" ).getValue() ).getReferencedPropertyName() )
						.as( child.getSimpleName() )
						.isEqualTo( "code" );
				softly.assertThat( binding.getTable().getColumns() )
						.as( child.getSimpleName() )
						.extracting( Column::getName )
						.containsExactly( "id" );
			}
		} );
	}

	private static void verifyPropertyRefConstraints(Metadata metadata) {
		verifyPropertyRef( metadata );

		// the primary key holds a foreign key, which is a constraint only when the association is constrained
		final var constrainedChild = metadata.getEntityBinding( ConstrainedRefChild.class.getName() );
		assertThat( constrainedChild.getProperty( "parent" ).getValue() ).isInstanceOf( ManyToOne.class );
		assertThat( constrainedChild.getTable().getForeignKeyCollection() ).singleElement().satisfies( foreignKey -> {
			assertThat( foreignKey.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
			assertThat( foreignKey.getReferencedEntityName() ).isEqualTo( CodedParent.class.getName() );
			assertThat( foreignKey.getReferencedColumns() ).extracting( Column::getName ).containsExactly( "code" );
		} );
		final var child = metadata.getEntityBinding( RefChild.class.getName() );
		assertThat( child.getProperty( "parent" ).getValue() ).isInstanceOf( ManyToOne.class );
		assertThat( child.getTable().getForeignKeyCollection() ).isEmpty();
	}

	/**
	 * Write the mapping as XML and read it back,
	 * validating it against the schema, as the Gradle plugin does.
	 */
	private static Metadata roundTrip(String mapping, ServiceRegistryScope scope) throws JAXBException {
		final var registry = scope.getRegistry();
		final var binder = new MappingBinder( registry );
		final var binding = binder.bind( registry.requireService( ClassLoaderService.class ).locateResourceStream( mapping ),
				new Origin( SourceType.RESOURCE, mapping ) );
		final var xml = new ByteArrayOutputStream();
		new MappingBinder( registry ).mappingJaxbContext().createMarshaller()
				.marshal( binding.getRoot(), xml );
		return MetadataBuildingHelper.buildMetadata( registry, new MappingSources().addXmlMappingSource( XmlMappingSource.fromInputStream( new ByteArrayInputStream( xml.toByteArray() ) ) ) );
	}

	@SuppressWarnings("unused")
	public static class Parent {
		private Long id;
	}

	@SuppressWarnings("unused")
	public static class Child {
		private Long id;
		private Parent parent;
	}

	@SuppressWarnings("unused")
	public static class CodedParent {
		private Long id;
		private Long code;
	}

	@SuppressWarnings("unused")
	public static class ConstrainedRefChild {
		private Long id;
		private CodedParent parent;
	}

	@SuppressWarnings("unused")
	public static class RefChild {
		private Long id;
		private CodedParent parent;
	}
}
