/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@RunWith( BytecodeEnhancerRunner.class )
public class MultipleBagsInLazyFetchGroupTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{StringsEntity.class};
    }

    @Before
    public void checkSettings() {
        assertTrue( sessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
    }

    @Before
    public void prepare() {
        doInJPA( this::sessionFactory, em -> {
            StringsEntity entity = new StringsEntity();
            entity.id = 1L;
            entity.text = "abc";
            entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
            entity.someStrings2 = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
            em.persist( entity );
        } );
    }

    @Test
    public void test() {
        doInJPA( this::sessionFactory, entityManager -> {
            StringsEntity entity = entityManager.getReference( StringsEntity.class, 1L );
            assertEquals( 3, entity.someStrings.size() );
            assertEquals( 3, entity.someStrings2.size() );
        } );
    }

    // --- //

    @Entity
    @Table( name = "STRINGS_ENTITY" )
    private static class StringsEntity {

        @Id
        Long id;

        String text;

        @ElementCollection(fetch = FetchType.EAGER)
        List<String> someStrings;

        @ElementCollection(fetch = FetchType.EAGER)
        List<String> someStrings2;
    }
}
