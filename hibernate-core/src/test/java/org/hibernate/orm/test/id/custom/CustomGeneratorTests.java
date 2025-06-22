/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.custom;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = TheEntity.class )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSequences.class )
public class CustomGeneratorTests {
	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntity.class, (descriptor) -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();

//			final String strategy = value.getIdentifierGeneratorStrategy();
//			assertThat( strategy ).isEqualTo( "assigned" );
		} );
	}

	@Test
	public void basicUseTest(SessionFactoryScope scope) {
		assertThat( CustomSequenceGenerator.generationCount ).isEqualTo( 0 );

		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( "steve" ) );
		} );

		assertThat( CustomSequenceGenerator.generationCount ).isEqualTo( 1 );
	}
}
