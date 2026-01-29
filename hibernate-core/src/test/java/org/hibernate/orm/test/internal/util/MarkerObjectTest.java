/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.internal.util.MarkerObject;
import org.hibernate.testing.orm.junit.BaseUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MarkerObject} to ensure proper singleton behavior
 * across serialization and deserialization.
 *
 * <p>This addresses HHH-9414 where marker object identity was lost after
 * deserialization in scenarios like session serialization (Spring Web Flow)
 * or clustered environments, causing reference-equality checks to fail.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-9414">HHH-9414</a>
 */
@BaseUnitTest
public class MarkerObjectTest {

	@Test
	public void testNoRowSerializationPreservesIdentity() throws Exception {
		MarkerObject original = MarkerObject.NO_ROW;
		MarkerObject deserialized = serializeDeserialize(original);

		// Verify singleton identity is preserved
		assertSame(original, deserialized, "Deserialized NO_ROW should be same instance as original");
		assertTrue(original == deserialized, "Reference equality (==) should work after deserialization");
	}

	@Test
	public void testInvokeImplementationSerializationPreservesIdentity() throws Exception {
		MarkerObject original = MarkerObject.INVOKE_IMPLEMENTATION;
		MarkerObject deserialized = serializeDeserialize(original);

		assertSame(original, deserialized, "Deserialized INVOKE_IMPLEMENTATION should be same instance");
		assertTrue(original == deserialized, "Reference equality should work after deserialization");
	}

	@Test
	public void testUnknownSerializationPreservesIdentity() throws Exception {
		MarkerObject original = MarkerObject.UNKNOWN;
		MarkerObject deserialized = serializeDeserialize(original);

		assertSame(original, deserialized, "Deserialized UNKNOWN should be same instance");
		assertTrue(original == deserialized, "Reference equality should work after deserialization");
	}

	@Test
	public void testUnfetchedCollectionSerializationPreservesIdentity() throws Exception {
		MarkerObject original = MarkerObject.UNFETCHED_COLLECTION;
		MarkerObject deserialized = serializeDeserialize(original);

		assertSame(original, deserialized, "Deserialized UNFETCHED_COLLECTION should be same instance");
		assertTrue(original == deserialized, "Reference equality should work after deserialization");
	}

	@Test
	public void testNullDiscriminatorSerializationPreservesIdentity() throws Exception {
		MarkerObject original = MarkerObject.NULL_DISCRIMINATOR;
		MarkerObject deserialized = serializeDeserialize(original);

		assertSame(original, deserialized, "Deserialized NULL_DISCRIMINATOR should be same instance");
		assertTrue(original == deserialized, "Reference equality should work after deserialization");
	}

	@Test
	public void testNotNullDiscriminatorSerializationPreservesIdentity() throws Exception {
		MarkerObject original = MarkerObject.NOT_NULL_DISCRIMINATOR;
		MarkerObject deserialized = serializeDeserialize(original);

		assertSame(original, deserialized, "Deserialized NOT_NULL_DISCRIMINATOR should be same instance");
		assertTrue(original == deserialized, "Reference equality should work after deserialization");
	}

	@Test
	public void testToStringPreserved() throws Exception {
		MarkerObject original = MarkerObject.NO_ROW;
		MarkerObject deserialized = serializeDeserialize(original);

		assertSame(original.toString(), deserialized.toString(),
				"toString() should return same value after deserialization");
	}

	/**
	 * Helper method to serialize and deserialize a MarkerObject.
	 */
	private MarkerObject serializeDeserialize(MarkerObject marker) throws Exception {
		// Serialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(marker);
		}

		byte[] bytes = baos.toByteArray();

		// Deserialize
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(bais)) {
			return (MarkerObject) ois.readObject();
		}
	}
}
