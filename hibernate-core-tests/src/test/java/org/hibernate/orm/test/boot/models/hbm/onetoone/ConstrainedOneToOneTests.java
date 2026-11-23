package org.hibernate.orm.test.boot.models.hbm.onetoone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import jakarta.xml.bind.JAXBException;

import org.hibernate.Hibernate;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * A {@code <one-to-one/>} which is constrained, or which has a {@code property-ref},
 * is joined on the primary key of its owner, whether or not the {@code hbm.xml}
 * mapping is transformed.
 */
@JiraKey("HHH-21112")
@SuppressWarnings("JUnitMalformedDeclaration")
public class ConstrainedOneToOneTests {
	private static final String MAPPING = "mappings/models/hbm/onetoone/constrained.hbm.xml";
	private static final String PROPERTY_REF_MAPPING = "mappings/models/hbm/onetoone/property-ref.hbm.xml";

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "false"))
	@DomainModel(xmlMappings = MAPPING)
	void testHbm(DomainModelScope scope) {
		verifySharedPrimaryKey( scope.getDomainModel() );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@DomainModel(xmlMappings = MAPPING)
	void testTransformed(DomainModelScope scope) {
		verifySharedPrimaryKey( scope.getDomainModel() );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "false"))
	void testTransformedAndSerialized(ServiceRegistryScope scope) throws JAXBException {
		verifySharedPrimaryKey( transformAndSerialize( MAPPING, scope ) );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "false"))
	@DomainModel(xmlMappings = PROPERTY_REF_MAPPING)
	void testPropertyRefHbm(DomainModelScope scope) {
		verifyPropertyRef( scope.getDomainModel() );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@DomainModel(xmlMappings = PROPERTY_REF_MAPPING)
	void testPropertyRefTransformed(DomainModelScope scope) {
		verifyTransformedPropertyRef( scope.getDomainModel() );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "false"))
	void testPropertyRefTransformedAndSerialized(ServiceRegistryScope scope) throws JAXBException {
		verifyTransformedPropertyRef( transformAndSerialize( PROPERTY_REF_MAPPING, scope ) );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@DomainModel(xmlMappings = PROPERTY_REF_MAPPING)
	@SessionFactory
	void testPropertyRefTransformedJoin(SessionFactoryScope scope) {
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

	private static void verifyTransformedPropertyRef(Metadata metadata) {
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
	 * Transform the mapping, write it as XML, and read it back,
	 * validating it against the schema, as the Gradle plugin does.
	 */
	private static Metadata transformAndSerialize(String mapping, ServiceRegistryScope scope) throws JAXBException {
		final var registry = scope.getRegistry();
		final var sources = new MetadataSources( registry ).addResource( mapping );
		final var transformed = HbmXmlTransformer.transform(
				sources.getHbmXmlBindings(),
				(MetadataImplementor) sources.buildMetadata(),
				UnsupportedFeatureHandling.ERROR
		);
		final var xml = new ByteArrayOutputStream();
		new MappingBinder( registry ).mappingJaxbContext().createMarshaller()
				.marshal( transformed.get( 0 ).getRoot(), xml );
		return new MetadataSources( registry )
				.addInputStream( new ByteArrayInputStream( xml.toByteArray() ) )
				.buildMetadata();
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
