/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
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
				PropertyHolder.class,
				PropertyHolder2.class,
		},
		annotatedPackageNames = "org.hibernate.orm.test.associations.any"
)
@SessionFactory
public class AnyTest {

	@Test
	public void test(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			//tag::associations-any-persist-example[]
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

			PropertyHolder namePropertyHolder = new PropertyHolder();
			namePropertyHolder.setId( 1L );
			namePropertyHolder.setProperty( nameProperty );

			session.persist( namePropertyHolder );
			//end::associations-any-persist-example[]
		} );

		scope.inTransaction( session -> {
			//tag::associations-any-query-example[]
			PropertyHolder propertyHolder = session.get( PropertyHolder.class, 1L );

			assertThat( propertyHolder.getProperty().getName() ).isEqualTo( "name" );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( "John Doe" );
			//end::associations-any-query-example[]
		} );
	}


	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-16938")
	public void testMetaAnnotated(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final StringProperty nameProperty = new StringProperty();
			nameProperty.setId( 2L );
			nameProperty.setName( "name2" );
			nameProperty.setValue( "Mario Rossi" );
			session.persist( nameProperty );
			final PropertyHolder2 namePropertyHolder = new PropertyHolder2();
			namePropertyHolder.setId( 2L );
			namePropertyHolder.setProperty( nameProperty );
			session.persist( namePropertyHolder );
		} );
		scope.inTransaction( session -> {
			final PropertyHolder2 propertyHolder = session.get( PropertyHolder2.class, 2L );
			assertThat( propertyHolder.getProperty().getName() ).isEqualTo( "name2" );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( "Mario Rossi" );
			final String propertyType = session.createNativeQuery(
					"select property_type from property_holder2",
					String.class
			).getSingleResult();
			assertThat( propertyType ).isEqualTo( "S" );
		} );
	}
}
