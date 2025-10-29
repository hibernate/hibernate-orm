/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Brett Meyer
 */
public class TypedValueSerializationTest {

	@Test
	@JiraKey(value = "HHH-9024")
	public void testTypedValueSerialization() throws Exception {
		final Type mockType = mock( Type.class );
		final String value = "foo";
		final TypedValue typedValue = new TypedValue( mockType, value );

		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (final ObjectOutputStream oos = new ObjectOutputStream( baos )) {
				oos.writeObject( typedValue );

				try (final ObjectInputStream ois = new ObjectInputStream(
						new ByteArrayInputStream( baos.toByteArray() ) )) {
					final TypedValue typedValueClone = (TypedValue) ois.readObject();

					assertEquals( typedValue.hashCode(), typedValueClone.hashCode() );
					assertEquals( typedValue.toString(), typedValueClone.toString() );
					assertEquals( typedValue.getValue(), typedValueClone.getValue() );
				}
			}
		}
	}

}
