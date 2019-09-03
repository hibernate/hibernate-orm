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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.CustomType;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UUIDCharType;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

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
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		BasicTypeRegistry registry = typeConfiguration.getBasicTypeRegistry();

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
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		BasicTypeRegistry registry = typeConfiguration.getBasicTypeRegistry();

		BasicType type = registry.getRegisteredType( SomeNoopType.INSTANCE.getName() );
		assertNull( type );

		registry.register( SomeNoopType.INSTANCE );
		type = registry.getRegisteredType( SomeNoopType.INSTANCE.getName() );
		assertNotNull( type );
		assertSame( SomeNoopType.INSTANCE, type );
	}

	@Test
	public void testRegisteringUserTypes() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		BasicTypeRegistry registry = typeConfiguration.getBasicTypeRegistry();

		registry.register( new TotallyIrrelevantUserType(), "key" );
		BasicType type = registry.getRegisteredType( "key" );
		assertNotNull( type );
		assertEquals( CustomType.class, type.getClass() );
		assertEquals( TotallyIrrelevantUserType.class, ( (CustomType) type ).getUserType().getClass() );

		type = registry.getRegisteredType( UUID.class.getName() );
		assertSame( UUIDBinaryType.INSTANCE, type );
		registry.register( new TotallyIrrelevantUserType(), UUID.class.getName() );
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

		@Override
		public int[] sqlTypes() {
			return new int[0];
		}

		@Override
		public Class returnedClass() {
			return null;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			return false;
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return 0;
		}

		@Override
		public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
			return null;
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return null;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return null;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return null;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return null;
		}
	}

}
