package org.hibernate.test.lob;

import java.sql.Types;

import org.hibernate.type.AbstractBynaryType;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MaterializedBlobType extends AbstractBynaryType {

	public int sqlType() {
		return Types.BLOB;
	}

	public String getName() {
		return "materialized-blob";
	}

	public Class getReturnedClass() {
		return byte[].class;
	}

	protected Object toExternalFormat(byte[] bytes) {
		return bytes;
	}

	protected byte[] toInternalFormat(Object bytes) {
		return ( byte[] ) bytes;
	}
}
