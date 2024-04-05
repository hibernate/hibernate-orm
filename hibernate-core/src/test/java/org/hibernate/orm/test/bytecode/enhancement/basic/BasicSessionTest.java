/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Luis Barreiro
 */
@DomainModel(
        annotatedClasses = {
                BasicSessionTest.MyEntity.class,
        }
)
@SessionFactory
@BytecodeEnhanced
public class BasicSessionTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            s.save( new MyEntity( 1L ) );
            s.save( new MyEntity( 2L ) );
        } );

        MyEntity[] entities = new MyEntity[2];

        scope.inTransaction( s -> {
            entities[0] = s.get( MyEntity.class, 1L );
            entities[1] = s.get( MyEntity.class, 2L );

            assertNotNull( entities[0].$$_hibernate_getEntityInstance() );
            assertSame( entities[0], entities[0].$$_hibernate_getEntityInstance() );
            assertNotNull( entities[0].$$_hibernate_getEntityEntry() );
            assertNull( entities[0].$$_hibernate_getPreviousManagedEntity() );
            assertNotNull( entities[0].$$_hibernate_getNextManagedEntity() );

            assertNotNull( entities[1].$$_hibernate_getEntityInstance() );
            assertSame( entities[1], entities[1].$$_hibernate_getEntityInstance() );
            assertNotNull( entities[1].$$_hibernate_getEntityEntry() );
            assertNotNull( entities[1].$$_hibernate_getPreviousManagedEntity() );
            assertNull( entities[1].$$_hibernate_getNextManagedEntity() );

            s.createQuery( "delete MyEntity" ).executeUpdate();
        } );

        assertNull( entities[0].$$_hibernate_getEntityEntry() );
        assertNull( entities[1].$$_hibernate_getEntityEntry() );
    }

    // --- //

    @Entity( name = "MyEntity" )
    @Table( name = "MY_ENTITY" )
    static class MyEntity implements ManagedEntity {

        @Id
        Long id;

        @Transient
        private transient EntityEntry entityEntry;
        @Transient
        private transient ManagedEntity previous;
        @Transient
        private transient ManagedEntity next;

        MyEntity() {
        }

        MyEntity(Long id) {
            this.id = id;
        }

        @Override
        public Object $$_hibernate_getEntityInstance() {
            return this;
        }

        @Override
        public EntityEntry $$_hibernate_getEntityEntry() {
            return entityEntry;
        }

        @Override
        public void $$_hibernate_setEntityEntry(EntityEntry entityEntry) {
            this.entityEntry = entityEntry;
        }

        @Override
        public ManagedEntity $$_hibernate_getNextManagedEntity() {
            return next;
        }

        @Override
        public void $$_hibernate_setNextManagedEntity(ManagedEntity next) {
            this.next = next;
        }

        @Override
        public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
            return previous;
        }

        @Override
        public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {
            this.previous = previous;
        }

        @Override
        public void $$_hibernate_setUseTracker(boolean useTracker) {

        }

        @Override
        public boolean $$_hibernate_useTracker() {
            return false;
        }
    }
}
