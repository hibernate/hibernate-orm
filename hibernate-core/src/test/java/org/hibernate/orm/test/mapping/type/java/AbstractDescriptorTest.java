/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.util.TimeZone;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.internal.NonContextualLobCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDescriptorTest<T> extends BaseUnitTestCase {
	protected static class Data<T> {
		private final T originalValue;
		private final T copyOfOriginalValue;
		private final T differentValue;

		public Data(T originalValue, T copyOfOriginalValue, T differentValue) {
			this.originalValue = originalValue;
			this.copyOfOriginalValue = copyOfOriginalValue;
			this.differentValue = differentValue;
		}
	}

	private final JavaType<T> typeDescriptor;

	protected final WrapperOptions wrapperOptions = new WrapperOptions() {
		@Override
		public SharedSessionContractImplementor getSession() {
			return null;
		}

		public boolean useStreamForLobBinding() {
			return false;
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return 0;
		}

		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		public JdbcType remapSqlTypeDescriptor(JdbcType sqlTypeDescriptor) {
			return sqlTypeDescriptor;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return null;
		}

		private final Dialect dialect = new H2Dialect() {
			@Override
			public boolean useConnectionToCreateLob() {
				return false;
			}
		};

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return null;
		}

		@Override
		public FormatMapper getXmlFormatMapper() {
			return null;
		}

		@Override
		public FormatMapper getJsonFormatMapper() {
			return null;
		}
	};

	public AbstractDescriptorTest(JavaType<T> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
	}

	private Data<T> testData;

	@Before
	public void setUp() throws Exception {
		testData = getTestData();
	}

	protected JavaType<T> typeDescriptor() {
		return typeDescriptor;
	}

	protected abstract Data<T> getTestData();

	protected abstract boolean shouldBeMutable();

	protected boolean isIdentityDifferentFromEquality() {
		return true;
	}

	@Test
	public void testEquality() {
		if ( isIdentityDifferentFromEquality() ) {
			assertFalse( testData.originalValue == testData.copyOfOriginalValue );
		}
		assertTrue( typeDescriptor.areEqual( testData.originalValue, testData.originalValue ) );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, testData.copyOfOriginalValue ) );
		assertFalse( typeDescriptor.areEqual( testData.originalValue, testData.differentValue ) );
	}

	@Test
	public void testExternalization() {
		// ensure the symmetry of toString/fromString
		String externalized = typeDescriptor.toString( testData.originalValue );
		T consumed = typeDescriptor.fromString( externalized );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, consumed ) );
	}

	/**
	 * Check that wrapping/unwrapping a value that already has the expected type
	 * does not fail and returns a value that is considered equal.
	 */
	@Test
	@JiraKey("HHH-17466")
	public void testPassThrough() {
		assertTrue( typeDescriptor.areEqual(
				testData.originalValue,
				typeDescriptor.wrap( testData.originalValue, wrapperOptions )
		) );
		assertTrue( typeDescriptor.areEqual(
				testData.originalValue,
				typeDescriptor.unwrap( testData.originalValue, typeDescriptor.getJavaTypeClass(), wrapperOptions )
		) );
	}

	@Test
	public void testMutabilityPlan() {
		assertTrue( shouldBeMutable() == typeDescriptor.getMutabilityPlan().isMutable() );

		if ( testData.copyOfOriginalValue instanceof Clob
				|| testData.copyOfOriginalValue instanceof Blob ) {
			return;
		}

		T copy = typeDescriptor.getMutabilityPlan().deepCopy( testData.copyOfOriginalValue );
		assertTrue( typeDescriptor.areEqual( copy, testData.copyOfOriginalValue ) );
		if ( ! shouldBeMutable() ) {
			assertTrue( copy == testData.copyOfOriginalValue );
		}

		// ensure the symmetry of assemble/disassebly
		//		NOTE: these should not use Session, so we just pass null

		Serializable cached = typeDescriptor.getMutabilityPlan().disassemble( testData.copyOfOriginalValue, null );
		if ( ! shouldBeMutable() ) {
			assertTrue( cached == testData.copyOfOriginalValue );
		}
		T reassembled = typeDescriptor.getMutabilityPlan().assemble( cached, null );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, reassembled ) );
	}
}
