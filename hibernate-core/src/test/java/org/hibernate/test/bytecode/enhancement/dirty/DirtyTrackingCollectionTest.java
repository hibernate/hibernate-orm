/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.dirty;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-11293" )
@RunWith( BytecodeEnhancerRunner.class )
public class DirtyTrackingCollectionTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{StringsEntity.class};
    }

    @Before
    public void prepare() {
        doInJPA( this::sessionFactory, em -> {
            StringsEntity entity = new StringsEntity();
            entity.id = 1L;
            entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
            em.persist( entity );
        } );
    }

    @Test
    public void test() {
        doInJPA( this::sessionFactory, entityManager -> {
            StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
            entity.someStrings.clear();
        } );

        doInJPA( this::sessionFactory, entityManager -> {
            StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
            assertEquals( 0, entity.someStrings.size() );
            entity.someStrings.add( "d" );
        } );

        doInJPA( this::sessionFactory, entityManager -> {
            StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
            assertEquals( 1, entity.someStrings.size() );
            entity.someStrings = new ArrayList<>();
        } );

        doInJPA( this::sessionFactory, entityManager -> {
            StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
            assertEquals( 0, entity.someStrings.size() );
        } );
    }

    // --- //

    @Entity
    @Table( name = "STRINGS_ENTITY" )
    private static class StringsEntity {

        @Id
        Long id;

        @ElementCollection
        List<String> someStrings;
    }
}