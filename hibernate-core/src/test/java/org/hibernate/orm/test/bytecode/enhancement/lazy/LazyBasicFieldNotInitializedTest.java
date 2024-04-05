/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Gail Badner
 */
@JiraKey("HHH-9937")
@DomainModel(
        annotatedClasses = {
               LazyBasicFieldNotInitializedTest.TestEntity.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
                @Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
        }
)
@SessionFactory
@BytecodeEnhanced
public class LazyBasicFieldNotInitializedTest {

    private Long entityId;


    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            TestEntity entity = new TestEntity();
            entity.description = "desc";
            s.persist( entity );
            entityId = entity.id;
        } );
    }

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            TestEntity entity = s.get( TestEntity.class, entityId );
            assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );

            EntityPersister entityPersister = scope.getSessionFactory().getRuntimeMetamodels()
                    .getMappingMetamodel()
                    .getEntityDescriptor( TestEntity.class );

            boolean[] propertyLaziness = entityPersister.getPropertyLaziness();
            assertEquals( 1, propertyLaziness.length );
            Assertions.assertTrue( propertyLaziness[0] );

            // Make sure NonIdentifierAttribute#isLazy is consistent (HHH-10551)
            final AttributeMapping theBytesAttr = entityPersister.findAttributeMapping( "description" );
            assertThat( theBytesAttr ).isInstanceOf( BasicValuedModelPart.class );
            assertThat( theBytesAttr.getMappedFetchOptions().getTiming() ).isEqualTo( FetchTiming.DELAYED );
        } );
    }

    // --- //
    
    @Entity(name = "TestEntity")
    @Table( name = "TEST_ENTITY" )
    static class TestEntity {

        @Id
        @GeneratedValue
        Long id;

        @Basic( fetch = FetchType.LAZY )
        String description;
    }
}
