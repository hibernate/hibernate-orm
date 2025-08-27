/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.immutable;

import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@DomainModel( annotatedClasses = { Child.class, Parent.class } )
@SessionFactory
public class ImmutableManyToOneNaturalIdAnnotationTest {

	@Test
	@JiraKey( value = "HHH-10360")
	public void testNaturalIdNullability(SessionFactoryScope scope) {
		// nullability is not specified for either properties making up
		// the natural ID, so they should be nullable by annotation-specific default

		final RuntimeMetamodels runtimeMetamodels = scope.getSessionFactory().getRuntimeMetamodels();
		final EntityMappingType childMapping = runtimeMetamodels.getEntityMappingType( Child.class.getName() );

		final EntityPersister persister = childMapping.getEntityPersister();
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		final int nameIndex = entityMetamodel.getPropertyIndex( "name" );
		final int parentIndex = entityMetamodel.getPropertyIndex( "parent" );

		// checking alphabetic sort in relation to EntityPersister/EntityMetamodel
		assertThat( nameIndex, lessThan( parentIndex ) );

		assertFalse( persister.getPropertyUpdateability()[ nameIndex ] );
		assertFalse( persister.getPropertyUpdateability()[ parentIndex ] );

		assertTrue( persister.getPropertyNullability()[ nameIndex ] );
		assertTrue( persister.getPropertyNullability()[ parentIndex ] );


		final NaturalIdMapping naturalIdMapping = childMapping.getNaturalIdMapping();
		assertNotNull( naturalIdMapping );
		assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 2 ) );

		// access by list-index should again be alphabetically sorted
		final SingularAttributeMapping first = naturalIdMapping.getNaturalIdAttributes().get( 0 );
		assertThat( first.getAttributeName(), is( "name" ) );
		final AttributeMetadata firstMetadata = first.getAttributeMetadata();
		assertFalse( firstMetadata.getMutabilityPlan().isMutable() );

		final SingularAttributeMapping second = naturalIdMapping.getNaturalIdAttributes().get( 1 );
		assertThat( second.getAttributeName(), is( "parent" ) );
		final AttributeMetadata secondMetadata = second.getAttributeMetadata();
		assertFalse( secondMetadata.getMutabilityPlan().isMutable() );
	}
}
