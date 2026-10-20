package org.hibernate.orm.test.mapping.access;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-21017" )
@DomainModel( annotatedClasses = { Person.class, Name.class } )
@SessionFactory( exportSchema = false )
public class EmbeddableAccessTests {
	@Test
	public void verifyBootModel(DomainModelScope scope) {
		scope.withHierarchy( Person.class, (personDescriptor) -> {
			final Property nameProperty = personDescriptor.getProperty( "name" );

			final Component nameMapping = (Component) nameProperty.getValue();
			assertThat( nameMapping.getPropertySpan() ).isEqualTo( 2 );
			final Property nameFirst = nameMapping.getProperty( 0 );
			final Property nameLast = nameMapping.getProperty( 1 );
			assertThat( nameFirst.getName() ).isEqualTo( "first" );
			assertThat( nameLast.getName() ).isEqualTo( "last" );

			final Property aliasesProperty = personDescriptor.getProperty( "aliases" );
			final Component aliasMapping = (Component) ( (Collection) aliasesProperty.getValue() ).getElement();
			assertThat( aliasMapping.getPropertySpan() ).isEqualTo( 2 );
			final Property aliasFirst = aliasMapping.getProperty( 0 );
			final Property aliasLast = aliasMapping.getProperty( 1 );
			assertThat( aliasFirst.getName() ).isEqualTo( "first" );
			assertThat( aliasLast.getName() ).isEqualTo( "last" );
		} );
	}

	@Test
	public void verifyRuntimeModel(SessionFactoryScope scope) {
		final RuntimeMetamodels runtimeMetamodels = scope.getSessionFactory().getRuntimeMetamodels();
		final EntityMappingType personDescriptor = runtimeMetamodels.getMappingMetamodel().getEntityDescriptor( Person.class );

		// Both embeddables inherit Person's FIELD access despite PROPERTY overrides
		// on the embedding attributes, so their attribute names are first and last.
		final EmbeddableMappingType nameEmbeddable = ( (EmbeddedAttributeMapping) personDescriptor.findAttributeMapping( "name" ) ).getEmbeddableTypeDescriptor();
		assertThat( nameEmbeddable.getNumberOfAttributeMappings() ).isEqualTo( 2 );
		final AttributeMapping nameFirst = nameEmbeddable.getAttributeMapping( 0 );
		final AttributeMapping nameLast = nameEmbeddable.getAttributeMapping( 1 );
		assertThat( nameFirst.getAttributeName() ).isEqualTo( "first" );
		assertThat( nameLast.getAttributeName() ).isEqualTo( "last" );

		final PluralAttributeMapping aliasesAttribute = (PluralAttributeMapping) personDescriptor.findAttributeMapping( "aliases" );
		final EmbeddableMappingType aliasEmbeddable = ( (EmbeddedCollectionPart) aliasesAttribute.getElementDescriptor() ).getEmbeddableTypeDescriptor();
		assertThat( aliasEmbeddable.getNumberOfAttributeMappings() ).isEqualTo( 2 );
		final AttributeMapping aliasFirst = aliasEmbeddable.getAttributeMapping( 0 );
		final AttributeMapping aliasLast = aliasEmbeddable.getAttributeMapping( 1 );
		assertThat( aliasFirst.getAttributeName() ).isEqualTo( "first" );
		assertThat( aliasLast.getAttributeName() ).isEqualTo( "last" );
	}
}
