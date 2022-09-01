/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderService;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;

/**
 * @author Christian Beikov
 */
@TestForIssue( jiraKey = "HHH-14348" )
@RunWith( BytecodeEnhancerRunner.class )
public class DirtyTrackingCollectionInDefaultFetchGroupTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{StringsEntity.class};
    }

    @Override
    protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addService(
                SessionFactoryBuilderService.class,
                (SessionFactoryBuilderService) (metadata, bootstrapContext) -> {
                    SessionFactoryOptionsBuilder optionsBuilder = new SessionFactoryOptionsBuilder(
                            metadata.getMetadataBuildingOptions().getServiceRegistry(),
                            bootstrapContext
                    );
                    optionsBuilder.enableCollectionInDefaultFetchGroup( true );
                    return new SessionFactoryBuilderImpl( metadata, optionsBuilder );
                }
        );
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
            entityManager.flush();
            assertFalse( Hibernate.isInitialized( entity.someStrings ) );
            assertFalse( Hibernate.isInitialized( entity.someStringEntities ) );
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

        @ManyToOne(fetch = FetchType.LAZY)
        StringsEntity parent;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
        Set<StringsEntity> someStringEntities;
    }
}
