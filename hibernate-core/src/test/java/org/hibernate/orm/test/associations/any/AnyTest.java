/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class AnyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			IntegerProperty.class,
			StringProperty.class,
			PropertyHolder.class,
			PropertyHolder2.class,
		};
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] {
			getClass().getPackage().getName()
		};
	}

	@Test
	public void test() {

		doInHibernate(this::sessionFactory, session -> {
			//tag::associations-any-persist-example[]
			IntegerProperty ageProperty = new IntegerProperty();
			ageProperty.setId(1L);
			ageProperty.setName("age");
			ageProperty.setValue(23);

			session.persist(ageProperty);

			StringProperty nameProperty = new StringProperty();
			nameProperty.setId(1L);
			nameProperty.setName("name");
			nameProperty.setValue("John Doe");

			session.persist(nameProperty);

			PropertyHolder namePropertyHolder = new PropertyHolder();
			namePropertyHolder.setId(1L);
			namePropertyHolder.setProperty(nameProperty);

			session.persist(namePropertyHolder);
			//end::associations-any-persist-example[]
		});

		doInHibernate(this::sessionFactory, session -> {
			//tag::associations-any-query-example[]
			PropertyHolder propertyHolder = session.get(PropertyHolder.class, 1L);

			assertEquals("name", propertyHolder.getProperty().getName());
			assertEquals("John Doe", propertyHolder.getProperty().getValue());
			//end::associations-any-query-example[]
		});
	}


	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16938" )
	public void testMetaAnnotated() {
		doInHibernate( this::sessionFactory, session -> {
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
		doInHibernate( this::sessionFactory, session -> {
			final PropertyHolder2 propertyHolder = session.get( PropertyHolder2.class, 2L );
			assertEquals( "name2", propertyHolder.getProperty().getName() );
			assertEquals( "Mario Rossi", propertyHolder.getProperty().getValue() );
			final String propertyType = session.createNativeQuery(
					"select property_type from property_holder2",
					String.class
			).getSingleResult();
			assertEquals( "S", propertyType );
		} );
	}
}
