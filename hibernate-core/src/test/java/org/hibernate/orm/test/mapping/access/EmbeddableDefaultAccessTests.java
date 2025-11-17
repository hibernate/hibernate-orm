/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import org.hibernate.mapping.BasicValue;
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
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { Person2.class, Name2.class } )
@SessionFactory( exportSchema = false )
public class EmbeddableDefaultAccessTests {
	@Test
	public void verifyBootModel(DomainModelScope scope) {
		scope.withHierarchy( Person2.class, (personDescriptor) -> {
			final Property nameProperty = personDescriptor.getProperty( "name" );
			final Component nameMapping = (Component) nameProperty.getValue();
			assertThat( nameMapping.getPropertySpan() ).isEqualTo( 2 );
			final Property nameFirst = nameMapping.getProperty( 0 );
			final Property nameLast = nameMapping.getProperty( 1 );
			assertThat( nameFirst.getName() ).isEqualTo( "first" );
			assertThat( nameLast.getName() ).isEqualTo( "last" );
			assertThat( ( (BasicValue) nameFirst.getValue() ).getColumn().getText() ).isEqualTo( "first_name" );
			assertThat( ( (BasicValue) nameLast.getValue() ).getColumn().getText() ).isEqualTo( "last_name" );
			assertThat( ( (BasicValue) nameFirst.getValue() ).getJpaAttributeConverterDescriptor() ).isNotNull();

			final Property aliasesProperty = personDescriptor.getProperty( "aliases" );
			final Component aliasMapping = (Component) ( (Collection) aliasesProperty.getValue() ).getElement();
			assertThat( aliasMapping.getPropertySpan() ).isEqualTo( 2 );
			final Property aliasFirst = aliasMapping.getProperty( 0 );
			final Property aliasLast = aliasMapping.getProperty( 1 );
			assertThat( aliasFirst.getName() ).isEqualTo( "first" );
			assertThat( aliasLast.getName() ).isEqualTo( "last" );
			assertThat( ( (BasicValue) aliasFirst.getValue() ).getColumn().getText() ).isEqualTo( "first_name" );
			assertThat( ( (BasicValue) aliasLast.getValue() ).getColumn().getText() ).isEqualTo( "last_name" );
			assertThat( ( (BasicValue) aliasFirst.getValue() ).getJpaAttributeConverterDescriptor() ).isNotNull();
		} );
	}

	@Test
	public void verifyRuntimeModel(SessionFactoryScope scope) {
		final RuntimeMetamodels runtimeMetamodels = scope.getSessionFactory().getRuntimeMetamodels();
		final EntityMappingType personDescriptor = runtimeMetamodels.getEntityMappingType( Person2.class );

		// Person defines FIELD access, while Name uses PROPERTY
		//		- if we find the property annotations, the attribute names will be
		//			`firstName` and `lastName`, and the columns `first_name` and `last_name`
		//		- otherwise, we have property and column names being `first` and `last`
		final EmbeddableMappingType nameEmbeddable = ( (EmbeddedAttributeMapping) personDescriptor.findAttributeMapping( "name" ) ).getEmbeddableTypeDescriptor();
		assertThat( nameEmbeddable.getNumberOfAttributeMappings() ).isEqualTo( 2 );
		final AttributeMapping nameFirst = nameEmbeddable.getAttributeMapping( 0 );
		final AttributeMapping nameLast = nameEmbeddable.getAttributeMapping( 1 );
		assertThat( nameFirst.getAttributeName() ).isEqualTo( "first" );
		assertThat( nameLast.getAttributeName() ).isEqualTo( "last" );

		final PluralAttributeMapping aliasesAttribute = (PluralAttributeMapping) personDescriptor.findAttributeMapping( "aliases" );
		final EmbeddableMappingType aliasEmbeddable = ( (EmbeddedCollectionPart) aliasesAttribute.getElementDescriptor() ).getEmbeddableTypeDescriptor();
		assertThat( aliasEmbeddable.getNumberOfAttributeMappings() ).isEqualTo( 2 );
		final AttributeMapping aliasFirst = nameEmbeddable.getAttributeMapping( 0 );
		final AttributeMapping aliasLast = nameEmbeddable.getAttributeMapping( 1 );
		assertThat( aliasFirst.getAttributeName() ).isEqualTo( "first" );
		assertThat( aliasLast.getAttributeName() ).isEqualTo( "last" );
	}
}
