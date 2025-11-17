/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				IntegerProperty.class,
				StringProperty.class,
				PropertyRepository.class
		},
		annotatedPackageNames = "org.hibernate.orm.test.associations.any"
)
@SessionFactory
public class ManyToAnyTest {

	@Test
	public void test(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			//tag::associations-many-to-any-persist-example[]
			IntegerProperty ageProperty = new IntegerProperty();
			ageProperty.setId( 1L );
			ageProperty.setName( "age" );
			ageProperty.setValue( 23 );

			session.persist( ageProperty );

			StringProperty nameProperty = new StringProperty();
			nameProperty.setId( 1L );
			nameProperty.setName( "name" );
			nameProperty.setValue( "John Doe" );

			session.persist( nameProperty );

			PropertyRepository propertyRepository = new PropertyRepository();
			propertyRepository.setId( 1L );

			propertyRepository.getProperties().add( ageProperty );
			propertyRepository.getProperties().add( nameProperty );

			session.persist( propertyRepository );
			//end::associations-many-to-any-persist-example[]
		} );

		scope.inTransaction( session -> {
			//tag::associations-many-to-any-query-example[]
			PropertyRepository propertyRepository = session.get( PropertyRepository.class, 1L );

			assertThat( propertyRepository.getProperties().size() ).isEqualTo( 2 );

			for ( Property property : propertyRepository.getProperties() ) {
				assertThat( property.getValue() ).isNotNull();
			}
			//end::associations-many-to-any-query-example[]
		} );
	}


}
