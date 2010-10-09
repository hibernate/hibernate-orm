/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;

import junit.framework.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDescriptorTest<T> extends TestCase {
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testData = getTestData();
	}

	protected abstract Data<T> getTestData();

	protected abstract boolean shouldBeMutable();

	public void testEquality() {
		assertFalse( testData.originalValue == testData.copyOfOriginalValue );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, testData.originalValue ) );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, testData.copyOfOriginalValue ) );
		assertFalse( typeDescriptor.areEqual( testData.originalValue, testData.differentValue ) );
	}

	public void testExternalization() {
		// ensure the symmetry of toString/fromString
		String externalized = typeDescriptor.toString( testData.originalValue );
		T consumed = typeDescriptor.fromString( externalized );
		assertTrue( typeDescriptor.areEqual( testData.originalValue, consumed ) );
	}

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
