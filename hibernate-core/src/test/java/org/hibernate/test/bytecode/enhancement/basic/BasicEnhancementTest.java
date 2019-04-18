/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class BasicEnhancementTest {

    @Test
    public void basicManagedTest() {
        SimpleEntity entity = new SimpleEntity();

        // Call the new ManagedEntity methods
        assertTyping( ManagedEntity.class, entity );
        ManagedEntity managedEntity = (ManagedEntity) entity;
        assertSame( entity, managedEntity.$$_hibernate_getEntityInstance() );

        assertNull( managedEntity.$$_hibernate_getEntityEntry() );
        managedEntity.$$_hibernate_setEntityEntry( EnhancerTestUtils.makeEntityEntry() );
        assertNotNull( managedEntity.$$_hibernate_getEntityEntry() );
        managedEntity.$$_hibernate_setEntityEntry( null );
        assertNull( managedEntity.$$_hibernate_getEntityEntry() );

        managedEntity.$$_hibernate_setNextManagedEntity( managedEntity );
        managedEntity.$$_hibernate_setPreviousManagedEntity( managedEntity );
        assertSame( managedEntity, managedEntity.$$_hibernate_getNextManagedEntity() );
        assertSame( managedEntity, managedEntity.$$_hibernate_getPreviousManagedEntity() );
    }

    @Test
    public void basicInterceptableTest() {
        SimpleEntity entity = new SimpleEntity();

        assertTyping( PersistentAttributeInterceptable.class, entity );
        PersistentAttributeInterceptable interceptableEntity = (PersistentAttributeInterceptable) entity;

        assertNull( interceptableEntity.$$_hibernate_getInterceptor() );
        interceptableEntity.$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );
        assertNotNull( interceptableEntity.$$_hibernate_getInterceptor() );

        assertNull( EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" ) );
        entity.setAnObject( new Object() );

        assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" ) );
        assertSame( ObjectAttributeMarkerInterceptor.READ_MARKER, entity.getAnObject() );

        entity.setAnObject( null );
        assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" ) );
    }

    @Test
    public void basicExtendedEnhancementTest() {
        // test uses ObjectAttributeMarkerInterceptor to ensure that field access is routed through enhanced methods

        SimpleEntity entity = new SimpleEntity();
        ( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );

        Object decoy = new Object();
        entity.anUnspecifiedObject = decoy;

        Object gotByReflection = EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" );
        assertNotSame( decoy, gotByReflection );
        assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, gotByReflection );

        Object entityObject = entity.anUnspecifiedObject;

        assertNotSame( decoy, entityObject );
        assertSame( ObjectAttributeMarkerInterceptor.READ_MARKER, entityObject );

        // do some more calls on the various types, without the interceptor
        ( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( null );

        entity.id = 1234567890L;
        Assert.assertEquals( 1234567890L, (long) entity.getId() );

        entity.name = "Entity Name";
        assertSame( "Entity Name", entity.name );

        entity.active = true;
        assertTrue( entity.getActive() );

        entity.someStrings = Arrays.asList( "A", "B", "C", "D" );
        assertArrayEquals( new String[]{"A", "B", "C", "D"}, entity.someStrings.toArray() );
    }

    // --- //

    public static class ObjectAttributeMarkerInterceptor implements PersistentAttributeInterceptor {

        public static final Object READ_MARKER = new Object();
        public static final Object WRITE_MARKER = new Object();

        @Override
        public boolean readBoolean(Object obj, String name, boolean oldValue) {
            return oldValue;
        }

        @Override
        public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
            return newValue;
        }

        @Override
        public byte readByte(Object obj, String name, byte oldValue) {
            return oldValue;
        }

        @Override
        public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
            return newValue;
        }

        @Override
        public char readChar(Object obj, String name, char oldValue) {
            return oldValue;
        }

        @Override
        public char writeChar(Object obj, String name, char oldValue, char newValue) {
            return newValue;
        }

        @Override
        public short readShort(Object obj, String name, short oldValue) {
            return oldValue;
        }

        @Override
        public short writeShort(Object obj, String name, short oldValue, short newValue) {
            return newValue;
        }

        @Override
        public int readInt(Object obj, String name, int oldValue) {
            return oldValue;
        }

        @Override
        public int writeInt(Object obj, String name, int oldValue, int newValue) {
            return newValue;
        }

        @Override
        public float readFloat(Object obj, String name, float oldValue) {
            return oldValue;
        }

        @Override
        public float writeFloat(Object obj, String name, float oldValue, float newValue) {
            return newValue;
        }

        @Override
        public double readDouble(Object obj, String name, double oldValue) {
            return oldValue;
        }

        @Override
        public double writeDouble(Object obj, String name, double oldValue, double newValue) {
            return newValue;
        }

        @Override
        public long readLong(Object obj, String name, long oldValue) {
            return oldValue;
        }

        @Override
        public long writeLong(Object obj, String name, long oldValue, long newValue) {
            return newValue;
        }

        @Override
        public Object readObject(Object obj, String name, Object oldValue) {
            return READ_MARKER;
        }

        @Override
        public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
            return WRITE_MARKER;
        }
    }

    // --- //

    @Entity
    private static class SimpleEntity {

        Object anUnspecifiedObject;

        @Id
        Long id;

        String name;

        Boolean active;

        List<String> someStrings;

        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        Object getAnObject() {
            return anUnspecifiedObject;
        }

        void setAnObject(Object providedObject) {
            this.anUnspecifiedObject = providedObject;
        }

        List<String> getSomeStrings() {
            return Collections.unmodifiableList( someStrings );
        }

        void setSomeStrings(List<String> someStrings) {
            this.someStrings = someStrings;
        }
    }
}
