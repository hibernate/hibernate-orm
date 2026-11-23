package org.hibernate.orm.test.mapping.type.java;

import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
public class PrimitiveByteArrayDescriptorTest extends AbstractDescriptorTest<byte[]> {

	private final byte[] original = new byte[] {1, 2, 3};

	private final byte[] copy = new byte[] {1, 2, 3};

	private final byte[] different = new byte[] {3, 2, 1};

	public PrimitiveByteArrayDescriptorTest() {
		super( PrimitiveByteArrayJavaType.INSTANCE );
	}

	@Override
	protected Data<byte[]> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}

	@Test
	public void testExtractLoggableRepresentation() {
		var javaType = PrimitiveByteArrayJavaType.INSTANCE;

		assertThat( javaType.extractLoggableRepresentation( null ) ).isEqualTo( "null" );
		assertThat( javaType.extractLoggableRepresentation( new byte[] {} ) ).isEqualTo( "byte[0]" );
		assertThat( javaType.extractLoggableRepresentation( original ) ).isEqualTo( "byte[3]" );
	}
}
