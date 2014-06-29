/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.bytecode.enhancement;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;

import org.hibernate.EntityMode;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.internal.DefaultEntityEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.Status;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import javax.persistence.ElementCollection;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class EnhancerTest extends BaseUnitTestCase {
	private static EnhancementContext enhancementContext = new EnhancementContext() {
		@Override
		public ClassLoader getLoadingClassLoader() {
			return getClass().getClassLoader();
		}

		@Override
		public boolean isEntityClass(CtClass classDescriptor) {
			return true;
		}

		@Override
		public boolean isCompositeClass(CtClass classDescriptor) {
			return false;
		}

		@Override
		public boolean doDirtyCheckingInline(CtClass classDescriptor) {
			return true;
		}

		@Override
		public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
			return true;
		}

		@Override
		public boolean isLazyLoadable(CtField field) {
			return true;
		}

        @Override
        public boolean isMappedCollection(CtField field) {
            try {
                return (field.getAnnotation(OneToMany.class) != null ||
                        field.getAnnotation(ManyToMany.class) != null ||
                        field.getAnnotation(ElementCollection.class) != null);
            }
            catch (ClassNotFoundException e) {
                return false;
            }
        }

        @Override
		public boolean isPersistentField(CtField ctField) {
			return true;
		}

		@Override
		public CtField[] order(CtField[] persistentFields) {
			return persistentFields;
		}
	};

	@Test
	public void testEnhancement() throws Exception {
		testFor( SimpleEntity.class );
		testFor( SubEntity.class );
	}

	private void testFor(Class entityClassToEnhance) throws Exception {
		Enhancer enhancer = new Enhancer( enhancementContext );
		CtClass entityCtClass = generateCtClassForAnEntity( entityClassToEnhance );
		byte[] original = entityCtClass.toBytecode();
		//byte[] enhanced = enhancer.enhance( entityCtClass.getName(), original );
        byte[] enhanced = enhancer.enhance( entityCtClass.getName(), original );
		assertFalse( "entity was not enhanced", Arrays.equals( original, enhanced ) );

		ClassLoader cl = new ClassLoader() { };
		ClassPool cp = new ClassPool( false );
		cp.appendClassPath( new LoaderClassPath( cl ) );
		CtClass enhancedCtClass = cp.makeClass( new ByteArrayInputStream( enhanced ) );
        enhancedCtClass.debugWriteFile("/tmp");
        //just for debugging
        Class addressClass = null;
        Class countryClass = null;
        if(entityClassToEnhance.getName().endsWith("SimpleEntity")) {
            CtClass addressCtClass = generateCtClassForAnEntity( Address.class );
            byte[] enhancedAddress = enhancer.enhanceComposite(Address.class.getName(), addressCtClass.toBytecode());
            CtClass enhancedCtClassAddress = cp.makeClass( new ByteArrayInputStream( enhancedAddress ) );
            enhancedCtClassAddress.debugWriteFile("/tmp");
            addressClass = enhancedCtClassAddress.toClass( cl, this.getClass().getProtectionDomain() );

            CtClass countryCtClass = generateCtClassForAnEntity( Country.class );
            byte[] enhancedCountry = enhancer.enhanceComposite(Country.class.getName(), countryCtClass.toBytecode());
            CtClass enhancedCtClassCountry = cp.makeClass( new ByteArrayInputStream( enhancedCountry ) );
            enhancedCtClassCountry.debugWriteFile("/tmp");
            countryClass = enhancedCtClassCountry.toClass( cl, this.getClass().getProtectionDomain() );

        }
		Class entityClass = enhancedCtClass.toClass( cl, this.getClass().getProtectionDomain() );
		Object entityInstance = entityClass.newInstance();

		assertTyping( ManagedEntity.class, entityInstance );

		// call the new methods
		//
		Method setter = entityClass.getMethod( EnhancerConstants.ENTITY_ENTRY_SETTER_NAME, EntityEntry.class );
		Method getter = entityClass.getMethod( EnhancerConstants.ENTITY_ENTRY_GETTER_NAME );
		assertNull( getter.invoke( entityInstance ) );
		setter.invoke( entityInstance, makeEntityEntry() );
		assertNotNull( getter.invoke( entityInstance ) );
		setter.invoke( entityInstance, new Object[] {null} );
		assertNull( getter.invoke( entityInstance ) );

		Method entityInstanceGetter = entityClass.getMethod( EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME );
		assertSame( entityInstance, entityInstanceGetter.invoke( entityInstance ) );

		Method previousGetter = entityClass.getMethod( EnhancerConstants.PREVIOUS_GETTER_NAME );
		Method previousSetter = entityClass.getMethod( EnhancerConstants.PREVIOUS_SETTER_NAME, ManagedEntity.class );
		previousSetter.invoke( entityInstance, entityInstance );
		assertSame( entityInstance, previousGetter.invoke( entityInstance ) );

		Method nextGetter = entityClass.getMethod( EnhancerConstants.PREVIOUS_GETTER_NAME );
		Method nextSetter = entityClass.getMethod( EnhancerConstants.PREVIOUS_SETTER_NAME, ManagedEntity.class );
		nextSetter.invoke( entityInstance, entityInstance );
		assertSame( entityInstance, nextGetter.invoke( entityInstance ) );

		// add an attribute interceptor...
		Method interceptorGetter = entityClass.getMethod( EnhancerConstants.INTERCEPTOR_GETTER_NAME );
		Method interceptorSetter = entityClass.getMethod( EnhancerConstants.INTERCEPTOR_SETTER_NAME, PersistentAttributeInterceptor.class );

		assertNull( interceptorGetter.invoke( entityInstance ) );
		entityClass.getMethod( "getId" ).invoke( entityInstance );

		interceptorSetter.invoke( entityInstance, new LocalPersistentAttributeInterceptor() );
		assertNotNull(interceptorGetter.invoke(entityInstance));

		// dirty checking is unfortunately just printlns for now... just verify the test output
		entityClass.getMethod( "getId" ).invoke( entityInstance );
		entityClass.getMethod( "setId", Long.class ).invoke( entityInstance, entityClass.getMethod( "getId" ).invoke( entityInstance ) );
		entityClass.getMethod( "setId", Long.class ).invoke( entityInstance, 1L );
        assertTrue((Boolean) entityClass.getMethod("$$_hibernate_hasDirtyAttributes").invoke(entityInstance));

		entityClass.getMethod( "isActive" ).invoke( entityInstance );
		entityClass.getMethod( "setActive", boolean.class ).invoke( entityInstance, entityClass.getMethod( "isActive" ).invoke( entityInstance ) );
		entityClass.getMethod( "setActive", boolean.class ).invoke(entityInstance, true);

		entityClass.getMethod( "getSomeNumber" ).invoke( entityInstance );
		entityClass.getMethod( "setSomeNumber", long.class ).invoke( entityInstance, entityClass.getMethod( "getSomeNumber" ).invoke( entityInstance ) );
		entityClass.getMethod( "setSomeNumber", long.class ).invoke(entityInstance, 1L);
        assertEquals(3, ((Set) entityClass.getMethod("$$_hibernate_getDirtyAttributes").invoke(entityInstance)).size());

        entityClass.getMethod("$$_hibernate_clearDirtyAttributes").invoke(entityInstance);
        //setting the same value should not make it dirty
        entityClass.getMethod( "setSomeNumber", long.class ).invoke(entityInstance, 1L);
        //assertEquals(0, ((Set) entityClass.getMethod("$$_hibernate_getDirtyAttributes").invoke(entityInstance)).size());
        //assertFalse((Boolean) entityClass.getMethod("$$_hibernate_hasDirtyAttributes").invoke(entityInstance));

        if(entityClass.getName().endsWith("SimpleEntity")) {

            List<String> strings = new ArrayList<String>();
            strings.add("FooBar");

            entityClass.getMethod( "setSomeStrings", java.util.List.class ).invoke(entityInstance, strings);

            assertTrue((Boolean) entityClass.getMethod("$$_hibernate_hasDirtyAttributes").invoke(entityInstance));

            Set tracked = (Set) entityClass.getMethod(EnhancerConstants.TRACKER_GET_NAME).invoke(entityInstance);
            assertEquals(1, tracked.size());
            assertEquals("someStrings", tracked.iterator().next());

            entityClass.getMethod("$$_hibernate_clearDirtyAttributes").invoke(entityInstance);

            ((List) entityClass.getMethod( "getSomeStrings").invoke(entityInstance)).add("JADA!");
            Boolean isDirty = (Boolean) entityClass.getMethod("$$_hibernate_hasDirtyAttributes").invoke(entityInstance);
            assertTrue(isDirty);
            assertEquals("someStrings",
                    ((Set) entityClass.getMethod(EnhancerConstants.TRACKER_GET_NAME).invoke(entityInstance)).iterator().next());
            entityClass.getMethod("$$_hibernate_clearDirtyAttributes").invoke(entityInstance);

            //this should not set the entity to dirty
            Set<Integer> ints = new HashSet<Integer>();
            ints.add(42);
            entityClass.getMethod( "setSomeInts", java.util.Set.class ).invoke(entityInstance, ints);
            isDirty = (Boolean) entityClass.getMethod("$$_hibernate_hasDirtyAttributes").invoke(entityInstance);
            assertFalse(isDirty);

            //testing composite object
            assert addressClass != null;
            Object address =  addressClass.newInstance();

            assert countryClass != null;
            Object country =  countryClass.newInstance();

            //Method adrInterceptorGetter = addressClass.getMethod( EnhancerConstants.INTERCEPTOR_GETTER_NAME );
            //Method adrInterceptorSetter = addressClass.getMethod( EnhancerConstants.INTERCEPTOR_SETTER_NAME, PersistentAttributeInterceptor.class );
            //adrInterceptorSetter.invoke( address, new LocalPersistentAttributeInterceptor() );

            entityClass.getMethod("setAddress", addressClass).invoke(entityInstance, address);

            addressClass.getMethod("setCity", String.class).invoke(address, "Arendal");

            tracked = (Set) entityClass.getMethod(EnhancerConstants.TRACKER_GET_NAME).invoke(entityInstance);
            assertEquals(2, tracked.size());
            Iterator iter = tracked.iterator();
            assertEquals("address", iter.next());
            assertEquals("address.city", iter.next());

            entityClass.getMethod("$$_hibernate_clearDirtyAttributes").invoke(entityInstance);

            //make sure that new composite instances are cleared
            Object address2 =  addressClass.newInstance();

            entityClass.getMethod("setAddress", addressClass).invoke(entityInstance, address2);
            addressClass.getMethod("setStreet1", String.class).invoke(address, "Heggedalveien");

            tracked = (Set) entityClass.getMethod(EnhancerConstants.TRACKER_GET_NAME).invoke(entityInstance);
            assertEquals(1, tracked.size());

            addressClass.getMethod("setCountry", countryClass).invoke(address2, country);
            countryClass.getMethod("setName", String.class).invoke(country, "Norway");

            tracked = (Set) entityClass.getMethod(EnhancerConstants.TRACKER_GET_NAME).invoke(entityInstance);
            assertEquals(3, tracked.size());
        }
	}

	private CtClass generateCtClassForAnEntity(Class entityClassToEnhance) throws Exception {
		ClassPool cp = new ClassPool( false );
		return cp.makeClass(
				getClass().getClassLoader().getResourceAsStream(
						entityClassToEnhance.getName().replace( '.', '/' ) + ".class"
				)
		);
	}

	private EntityEntry makeEntityEntry() {
		return new DefaultEntityEntry(
				Status.MANAGED,
				null,
				null,
				new Long(1),
				null,
				LockMode.NONE,
				false,
				null,
				false,
				false,
				null
		);
	}


	private class LocalPersistentAttributeInterceptor implements PersistentAttributeInterceptor {
		@Override
		public boolean readBoolean(Object obj, String name, boolean oldValue) {
			System.out.println( "Reading boolean [" + name + "]" );
			return oldValue;
		}

		@Override
		public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
			System.out.println( "Writing boolean [" + name + "]" );
			return newValue;
		}

		@Override
		public byte readByte(Object obj, String name, byte oldValue) {
			System.out.println( "Reading byte [" + name + "]" );
			return oldValue;
		}

		@Override
		public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
			System.out.println( "Writing byte [" + name + "]" );
			return newValue;
		}

		@Override
		public char readChar(Object obj, String name, char oldValue) {
			System.out.println( "Reading char [" + name + "]" );
			return oldValue;
		}

		@Override
		public char writeChar(Object obj, String name, char oldValue, char newValue) {
			System.out.println( "Writing char [" + name + "]" );
			return newValue;
		}

		@Override
		public short readShort(Object obj, String name, short oldValue) {
			System.out.println( "Reading short [" + name + "]" );
			return oldValue;
		}

		@Override
		public short writeShort(Object obj, String name, short oldValue, short newValue) {
			System.out.println( "Writing short [" + name + "]" );
			return newValue;
		}

		@Override
		public int readInt(Object obj, String name, int oldValue) {
			System.out.println( "Reading int [" + name + "]" );
			return oldValue;
		}

		@Override
		public int writeInt(Object obj, String name, int oldValue, int newValue) {
			System.out.println( "Writing int [" + name + "]" );
			return newValue;
		}

		@Override
		public float readFloat(Object obj, String name, float oldValue) {
			System.out.println( "Reading float [" + name + "]" );
			return oldValue;
		}

		@Override
		public float writeFloat(Object obj, String name, float oldValue, float newValue) {
			System.out.println( "Writing float [" + name + "]" );
			return newValue;
		}

		@Override
		public double readDouble(Object obj, String name, double oldValue) {
			System.out.println( "Reading double [" + name + "]" );
			return oldValue;
		}

		@Override
		public double writeDouble(Object obj, String name, double oldValue, double newValue) {
			System.out.println( "Writing double [" + name + "]" );
			return newValue;
		}

		@Override
		public long readLong(Object obj, String name, long oldValue) {
			System.out.println( "Reading long [" + name + "]" );
			return oldValue;
		}

		@Override
		public long writeLong(Object obj, String name, long oldValue, long newValue) {
			System.out.println( "Writing long [" + name + "]" );
			return newValue;
		}

		@Override
		public Object readObject(Object obj, String name, Object oldValue) {
			System.out.println( "Reading Object [" + name + "]" );
			return oldValue;
		}

		@Override
		public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
			System.out.println( "Writing Object [" + name + "]" );
			return newValue;
		}
	}
}
