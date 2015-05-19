/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UUIDCharType;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class BasicTypeRegistryTest extends BaseUnitTestCase {
	@Test
	public void testOverriding() {
		BasicTypeRegistry registry = new BasicTypeRegistry();

		BasicType type = registry.getRegisteredType( "uuid-binary" );
		assertSame( UUIDBinaryType.INSTANCE, type );
		type = registry.getRegisteredType( UUID.class.getName() );
		assertSame( UUIDBinaryType.INSTANCE, type );

		BasicType override = new UUIDCharType() {
			@Override
			protected boolean registerUnderJavaType() {
				return true;
			}
		};
		registry.register( override );
		type = registry.getRegisteredType( UUID.class.getName() );
		assertNotSame( UUIDBinaryType.INSTANCE, type );
		assertSame( override, type );
	}

	@Test
	public void testExpanding() {
		BasicTypeRegistry registry = new BasicTypeRegistry();

		BasicType type = registry.getRegisteredType( SomeNoopType.INSTANCE.getName() );
		assertNull( type );

		registry.register( SomeNoopType.INSTANCE );
		type = registry.getRegisteredType( SomeNoopType.INSTANCE.getName() );
		assertNotNull( type );
		assertSame( SomeNoopType.INSTANCE, type );
	}

	@Test
	public void testRegisteringUserTypes() {
		BasicTypeRegistry registry = new BasicTypeRegistry();

		registry.register( new TotallyIrrelevantUserType(), new String[] { "key" } );
		BasicType type = registry.getRegisteredType( "key" );
		assertNotNull( type );
		assertEquals( CustomType.class, type.getClass() );
		assertEquals( TotallyIrrelevantUserType.class, ( (CustomType) type ).getUserType().getClass() );

		registry.register( new TotallyIrrelevantCompositeUserType(), new String[] { "key" } );
		type = registry.getRegisteredType( "key" );
		assertNotNull( type );
		assertEquals( CompositeCustomType.class, type.getClass() );
		assertEquals( TotallyIrrelevantCompositeUserType.class, ( (CompositeCustomType) type ).getUserType().getClass() );

		type = registry.getRegisteredType( UUID.class.getName() );
		assertSame( UUIDBinaryType.INSTANCE, type );
		registry.register( new TotallyIrrelevantUserType(), new String[] { UUID.class.getName() } );
		type = registry.getRegisteredType( UUID.class.getName() );
		assertNotSame( UUIDBinaryType.INSTANCE, type );
		assertEquals( CustomType.class, type.getClass() );
	}

	public static class SomeNoopType extends AbstractSingleColumnStandardBasicType<String> {
		public static final SomeNoopType INSTANCE = new SomeNoopType();

		public SomeNoopType() {
			super( VarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
		}

		public String getName() {
			return "noop";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return false;
		}
	}

	public static class TotallyIrrelevantUserType implements UserType {

		public int[] sqlTypes() {
			return new int[0];
		}

		public Class returnedClass() {
			return null;
		}

		public boolean equals(Object x, Object y) throws HibernateException {
			return false;
		}

		public int hashCode(Object x) throws HibernateException {
			return 0;
		}

		public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
			return null;
		}

		public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		}

		public Object deepCopy(Object value) throws HibernateException {
			return null;
		}

		public boolean isMutable() {
			return false;
		}

		public Serializable disassemble(Object value) throws HibernateException {
			return null;
		}

		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return null;
		}

		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return null;
		}
	}

	public static class TotallyIrrelevantCompositeUserType implements CompositeUserType {

		public String[] getPropertyNames() {
			return new String[0];
		}

		public Type[] getPropertyTypes() {
			return new Type[0];
		}

		public Object getPropertyValue(Object component, int property) throws HibernateException {
			return null;
		}

		public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
		}

		public Class returnedClass() {
			return null;
		}

		public boolean equals(Object x, Object y) throws HibernateException {
			return false;
		}

		public int hashCode(Object x) throws HibernateException {
			return 0;
		}

		public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
				throws HibernateException, SQLException {
			return null;
		}

		public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
				throws HibernateException, SQLException {
		}

		public Object deepCopy(Object value) throws HibernateException {
			return null;
		}

		public boolean isMutable() {
			return false;
		}

		public Serializable disassemble(Object value, SessionImplementor session) throws HibernateException {
			return null;
		}

		public Object assemble(Serializable cached, SessionImplementor session, Object owner)
				throws HibernateException {
			return null;
		}

		public Object replace(Object original, Object target, SessionImplementor session, Object owner)
				throws HibernateException {
			return null;
		}
	}
}
