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

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@JiraKey( "HHH-14348" )
@DomainModel(
        annotatedClasses = {
               DirtyTrackingCollectionNotInDefaultFetchGroupTest.StringsEntity.class
        }
)
@SessionFactory(
        applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
public class DirtyTrackingCollectionNotInDefaultFetchGroupTest {

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        assertFalse( scope.getSessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
        scope.inTransaction( em -> {
            StringsEntity entity = new StringsEntity();
            entity.id = 1L;
            entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
            em.persist( entity );
        } );
    }

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( entityManager -> {
            StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
            entityManager.flush();
            BytecodeLazyAttributeInterceptor interceptor = (BytecodeLazyAttributeInterceptor) ( (PersistentAttributeInterceptable) entity )
                    .$$_hibernate_getInterceptor();
            assertTrue( interceptor.hasAnyUninitializedAttributes() );
            assertFalse( interceptor.isAttributeLoaded( "someStrings" ) );
            assertFalse( interceptor.isAttributeLoaded( "someStringEntities" ) );
        } );
    }

    // --- //

    @Entity
    @Table( name = "STRINGS_ENTITY" )
    static class StringsEntity {

        @Id
        Long id;

        @ElementCollection
        @CollectionTable(name = "STRINGS_ENTITY_SOME", joinColumns = @JoinColumn(name = "SOME_ID"))
        List<String> someStrings;

        @ManyToOne(fetch = FetchType.LAZY)
        StringsEntity parent;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
        Set<StringsEntity> someStringEntities;
    }
}
