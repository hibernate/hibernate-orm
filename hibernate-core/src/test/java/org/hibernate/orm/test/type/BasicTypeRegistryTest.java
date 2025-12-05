/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class BasicTypeRegistryTest {
	@Test
	public void testOverriding() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		BasicTypeRegistry registry = typeConfiguration.getBasicTypeRegistry();

		BasicType<?> uuidBinaryRegistration = registry.getRegisteredType( "uuid-binary" );
		assertInstanceOf( UUIDJavaType.class, uuidBinaryRegistration.getJavaTypeDescriptor() );
		assertInstanceOf( BinaryJdbcType.class, uuidBinaryRegistration.getJdbcType() );

		final BasicType<?> uuidRegistration = registry.getRegisteredType( UUID.class.getName() );
		assertInstanceOf( UUIDJavaType.class, uuidRegistration.getJavaTypeDescriptor() );
		assertInstanceOf( ObjectJdbcType.class, uuidRegistration.getJdbcType() );

		final BasicType<?> override = new BasicTypeImpl<>( UUIDJavaType.INSTANCE, CharJdbcType.INSTANCE );
		registry.register( override, UUID.class.getName() );

		final BasicType<?> overrideRegistration = registry.getRegisteredType( UUID.class.getName() );

		assertSame( override, overrideRegistration );
		assertNotSame( uuidBinaryRegistration, overrideRegistration );
		assertNotSame( uuidRegistration, overrideRegistration );
	}

	@Test
	public void testExpanding() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		BasicTypeRegistry registry = typeConfiguration.getBasicTypeRegistry();

		BasicType<?> type = registry.getRegisteredType( SomeNoopType.INSTANCE.getName() );
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
		BasicType<?> customType = registry.getRegisteredType( "key" );
		assertNotNull( customType );
		assertEquals( CustomType.class, customType.getClass() );
		assertEquals( TotallyIrrelevantUserType.class, ( (CustomType<Object>) customType ).getUserType().getClass() );

		BasicType<?> type = registry.getRegisteredType( UUID.class.getName() );
		assertThat( type.getJavaTypeDescriptor() ).isInstanceOf( UUIDJavaType.class );
		assertThat( type.getJdbcType() ).isInstanceOf( ObjectJdbcType.class );

		registry.register( new TotallyIrrelevantUserType(), UUID.class.getName() );
		assertNotSame( type, registry.getRegisteredType( UUID.class.getName() ) );
		assertEquals( CustomType.class, registry.getRegisteredType( UUID.class.getName() ).getClass() );
	}

	public static class SomeNoopType extends AbstractSingleColumnStandardBasicType<String> {
		public static final SomeNoopType INSTANCE = new SomeNoopType();

		public SomeNoopType() {
			super( VarcharJdbcType.INSTANCE, StringJavaType.INSTANCE );
		}

		public String getName() {
			return "noop";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return false;
		}
	}

	public static class TotallyIrrelevantUserType implements UserType<Object> {

		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		public Class<Object> returnedClass() {
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
		public Object nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
			return null;
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws HibernateException, SQLException {
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
