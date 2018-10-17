/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations.any;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class ManyToAnyTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            IntegerProperty.class,
            StringProperty.class,
            PropertyRepository.class
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

        doInHibernate( this::sessionFactory, session -> {
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

        doInHibernate( this::sessionFactory, session -> {
            //tag::associations-many-to-any-query-example[]
            PropertyRepository propertyRepository = session.get( PropertyRepository.class, 1L );

            assertEquals(2, propertyRepository.getProperties().size());

            for(Property property : propertyRepository.getProperties()) {
                assertNotNull( property.getValue() );
            }
            //end::associations-many-to-any-query-example[]
        } );
    }


}
