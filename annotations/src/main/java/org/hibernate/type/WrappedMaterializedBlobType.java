package org.hibernate.type;

/**
 * <tt>materialized_blob</tt>: A type that maps an SQL BLOB to Java Byte[].
 *
 * @author Strong Liu
 */
public class WrappedMaterializedBlobType extends MaterializedBlobType {
	public Class getReturnedClass() {
		return Byte[].class;
	}

	protected Object toExternalFormat(byte[] bytes) {
		if (bytes == null)
			return null;
		return wrapPrimitive(bytes);
	}

	protected byte[] toInternalFormat(Object bytes) {
		if (bytes == null)
			return null;
		return unwrapNonPrimitive((Byte[]) bytes);
	}

	private Byte[] wrapPrimitive(byte[] bytes) {
		int length = bytes.length;
		Byte[] result = new Byte[length];
		for (int index = 0; index < length; index++) {
			result[index] = Byte.valueOf(bytes[index]);
		}
		return result;
	}

	private byte[] unwrapNonPrimitive(Byte[] bytes) {
		int length = bytes.length;
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			result[i] = bytes[i].byteValue();
		}
		return result;
	}
}
