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
import java.util.Arrays;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;

import org.hibernate.EntityMode;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.Status;
import org.hibernate.mapping.PersistentClass;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

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
		byte[] enhanced = enhancer.enhance( entityCtClass.getName(), original );
		assertFalse( "entity was not enhanced", Arrays.equals( original, enhanced ) );

		ClassLoader cl = new ClassLoader() { };
		ClassPool cp = new ClassPool( false );
		cp.appendClassPath( new LoaderClassPath( cl ) );
		CtClass enhancedCtClass = cp.makeClass( new ByteArrayInputStream( enhanced ) );
		Class entityClass = enhancedCtClass.toClass( cl, this.getClass().getProtectionDomain() );
		Object entityInstance = entityClass.newInstance();

		assertTyping( ManagedEntity.class, entityInstance );

		// call the new methods
		//
		Method setter = entityClass.getMethod( Enhancer.ENTITY_ENTRY_SETTER_NAME, EntityEntry.class );
		Method getter = entityClass.getMethod( Enhancer.ENTITY_ENTRY_GETTER_NAME );
		assertNull( getter.invoke( entityInstance ) );
		setter.invoke( entityInstance, makeEntityEntry() );
		assertNotNull( getter.invoke( entityInstance ) );
		setter.invoke( entityInstance, new Object[] {null} );
		assertNull( getter.invoke( entityInstance ) );

		Method entityInstanceGetter = entityClass.getMethod( Enhancer.ENTITY_INSTANCE_GETTER_NAME );
		assertSame( entityInstance, entityInstanceGetter.invoke( entityInstance ) );

		Method previousGetter = entityClass.getMethod( Enhancer.PREVIOUS_GETTER_NAME );
		Method previousSetter = entityClass.getMethod( Enhancer.PREVIOUS_SETTER_NAME, ManagedEntity.class );
		previousSetter.invoke( entityInstance, entityInstance );
		assertSame( entityInstance, previousGetter.invoke( entityInstance ) );

		Method nextGetter = entityClass.getMethod( Enhancer.PREVIOUS_GETTER_NAME );
		Method nextSetter = entityClass.getMethod( Enhancer.PREVIOUS_SETTER_NAME, ManagedEntity.class );
		nextSetter.invoke( entityInstance, entityInstance );
		assertSame( entityInstance, nextGetter.invoke( entityInstance ) );

		// add an attribute interceptor...
		Method interceptorGetter = entityClass.getMethod( Enhancer.INTERCEPTOR_GETTER_NAME );
		Method interceptorSetter = entityClass.getMethod( Enhancer.INTERCEPTOR_SETTER_NAME, PersistentAttributeInterceptor.class );

		assertNull( interceptorGetter.invoke( entityInstance ) );
		entityClass.getMethod( "getId" ).invoke( entityInstance );

		interceptorSetter.invoke( entityInstance, new LocalPersistentAttributeInterceptor() );
		assertNotNull( interceptorGetter.invoke( entityInstance ) );

		// dirty checking is unfortunately just printlns for now... just verify the test output
		entityClass.getMethod( "getId" ).invoke( entityInstance );
		entityClass.getMethod( "setId", Long.class ).invoke( entityInstance, entityClass.getMethod( "getId" ).invoke( entityInstance ) );
		entityClass.getMethod( "setId", Long.class ).invoke( entityInstance, 1L );

		entityClass.getMethod( "isActive" ).invoke( entityInstance );
		entityClass.getMethod( "setActive", boolean.class ).invoke( entityInstance, entityClass.getMethod( "isActive" ).invoke( entityInstance ) );
		entityClass.getMethod( "setActive", boolean.class ).invoke( entityInstance, true );

		entityClass.getMethod( "getSomeNumber" ).invoke( entityInstance );
		entityClass.getMethod( "setSomeNumber", long.class ).invoke( entityInstance, entityClass.getMethod( "getSomeNumber" ).invoke( entityInstance ) );
		entityClass.getMethod( "setSomeNumber", long.class ).invoke( entityInstance, 1L );
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
		return new EntityEntry(
				Status.MANAGED,
				null,
				null,
				new Long(1),
				null,
				LockMode.NONE,
				false,
				null,
				EntityMode.POJO,
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
