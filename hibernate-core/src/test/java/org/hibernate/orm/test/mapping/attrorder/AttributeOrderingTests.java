/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attrorder;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { TheComponent.class, TheEntity.class },
		xmlMappings = "org/hibernate/orm/test/mapping/attrorder/mappings.hbm.xml"
)
@SessionFactory( exportSchema = false )
public class AttributeOrderingTests {
	@Test
	public void testOrdering(DomainModelScope modelScope, SessionFactoryScope sfScope) {
		// Force the creation of the session factory
		// We need this because properties are only sorted when finishing the initialization of the domain model
		SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory();

		// check the boot model.. it should have been sorted as part of calls to
		// prepare for mapping model creation

		verifyBootModel( modelScope );


		// Also check the mapping model *and* the persister model - these need to be in-sync as far as ordering

		final RuntimeMetamodels runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		verifyRuntimeEntityMapping( runtimeMetamodels.getEntityMappingType( TheEntity.class ) );
		verifyRuntimeEntityMapping( runtimeMetamodels.getEntityMappingType( "TheEntityHbm" ) );

	}

	public void verifyBootModel(DomainModelScope modelScope) {
		final Consumer<Iterator<Property>> alphabeticOrderChecker = properties -> {
			String last = null;
			while ( properties.hasNext() ) {
				final String current = properties.next().getName();
				assert last == null || last.compareTo( current ) < 0 : "not alphabetical : " + last + " -> " + current;

				last = current;
			}
		};

		modelScope.getDomainModel().getEntityBindings().forEach(
				binding -> alphabeticOrderChecker.accept( binding.getPropertyClosure().iterator() )
		);

		modelScope.getDomainModel().visitRegisteredComponents(
				binding -> alphabeticOrderChecker.accept( binding.getProperties().iterator() )
		);
	}

	public void verifyRuntimeEntityMapping(EntityMappingType entityMappingType) {
		final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();
		assertThat( naturalIdMapping, notNullValue() );
		assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 2 ) );
		assertThat( naturalIdMapping.getNaturalIdAttributes().get( 0 ).getAttributeName(), is( "assignment" ) );
		assertThat( naturalIdMapping.getNaturalIdAttributes().get( 1 ).getAttributeName(), is( "userCode" ) );

		final AttributeMappingsList attributeMappings = entityMappingType.getAttributeMappings();
		assertThat( attributeMappings.size(), is( 5 ) );

		assertThat( attributeMappings.get( 0 ).getAttributeName(), is( "assignment" ) );
		assertThat( entityMappingType.getEntityPersister().getPropertyNames()[ 0 ], is( "assignment" ) );

		assertThat( attributeMappings.get( 1 ).getAttributeName(), is( "name" ) );
		assertThat( entityMappingType.getEntityPersister().getPropertyNames()[ 1 ], is( "name" ) );

		final EmbeddedAttributeMapping theComponentAttrMapping = (EmbeddedAttributeMapping) attributeMappings.get( 2 );
		assertThat( theComponentAttrMapping.getAttributeName(), is( "theComponent" ) );
		assertThat( entityMappingType.getEntityPersister().getPropertyNames()[ 2 ], is( "theComponent" ) );

		final EmbeddableMappingType embeddable = theComponentAttrMapping.getMappedType();
		final AttributeMappingsList embeddableAttributeMappings = embeddable.getAttributeMappings();
		assertThat( embeddableAttributeMappings.get( 0 ).getAttributeName(), is( "nestedAnything" ) );
		assertThat( embeddableAttributeMappings.get( 1 ).getAttributeName(), is( "nestedName" ) );

		assertThat( attributeMappings.get( 3 ).getAttributeName(), is( "theComponents" ) );
		assertThat( entityMappingType.getEntityPersister().getPropertyNames()[ 3 ], is( "theComponents" ) );

		assertThat( attributeMappings.get( 4 ).getAttributeName(), is( "userCode" ) );
		assertThat( entityMappingType.getEntityPersister().getPropertyNames()[ 4 ], is( "userCode" ) );
	}
}
