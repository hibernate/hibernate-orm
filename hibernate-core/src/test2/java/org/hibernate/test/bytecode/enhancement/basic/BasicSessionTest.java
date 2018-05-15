/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class BasicSessionTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{ MyEntity.class};
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            s.save( new MyEntity( 1L ) );
            s.save( new MyEntity( 2L ) );
        } );

        MyEntity[] entities = new MyEntity[2];

        doInHibernate( this::sessionFactory, s -> {
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
    private static class MyEntity implements ManagedEntity {

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
    }
}
