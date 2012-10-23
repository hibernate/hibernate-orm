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

import javax.persistence.Entity;
import javax.persistence.Id;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.hibernate.EntityMode;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.Status;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

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
		public boolean isEntityClass(String className) {
			return true;
		}

		@Override
		public boolean isCompositeClass(String className) {
			return false;
		}

		@Override
		public boolean isPersistentField(CtField ctField) {
			return true;
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
		ClassPool cp2 = new ClassPool( false );
		cp2.appendClassPath( new LoaderClassPath( cl ) );
		CtClass enhancedCtClass = cp2.makeClass( new ByteArrayInputStream( enhanced ) );
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

}
