/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDescriptorTest<T> extends BaseUnitTestCase {
	protected class Data<T> {
		private final T originalValue;
		private final T copyOfOriginalValue;
		private final T differentValue;

		public Data(T originalValue, T copyOfOriginalValue, T differentValue) {
			this.originalValue = originalValue;
			this.copyOfOriginalValue = copyOfOriginalValue;
			this.differentValue = differentValue;
		}
	}

	private final JavaTypeDescriptor<T> typeDescriptor;

	public AbstractDescriptorTest(JavaTypeDescriptor<T> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
	}

	private Data<T> testData;

	@Before
	public void setUp() throws Exception {
		testData = getTestData();
	}

	protected abstract Data<T> getTestData();

	protected abstract boolean shouldBeMutable();

	@Test
	public void testEquality() {
		assertFalse( testData.originalValue == testData.copyOfOriginalValue );
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

	@Test
	public void testMutabilityPlan() {
		assertTrue( shouldBeMutable() == typeDescriptor.getMutabilityPlan().isMutable() );

		if ( Clob.class.isInstance( testData.copyOfOriginalValue )
				|| Blob.class.isInstance( testData.copyOfOriginalValue ) ) {
			return;
		}

		T copy = typeDescriptor.getMutabilityPlan().deepCopy( testData.copyOfOriginalValue );
		assertTrue( typeDescriptor.areEqual( copy, testData.copyOfOriginalValue ) );
		if ( ! shouldBeMutable() ) {
			assertTrue( copy == testData.copyOfOriginalValue );
		}

		// ensure the symmetry of assemble/disassebly
		Serializable cached = typeDescriptor.getMutabilityPlan().disassemble( testData.copyOfOriginalValue );
		if ( ! shouldBeMutable() ) {
			assertTrue( cached == testData.copyOfOriginalValue );
		}
		T reassembled = typeDescriptor.getMutabilityPlan().assemble( cached );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, reassembled ) );
	}
}
