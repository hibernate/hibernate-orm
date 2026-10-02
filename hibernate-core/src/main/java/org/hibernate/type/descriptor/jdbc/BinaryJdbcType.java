package org.hibernate.type.descriptor.jdbc;
import java.sql.Types;

/**
 * Descriptor for {@link Types#BINARY BINARY} handling.
 *
 * @author Steve Ebersole
 */
public class BinaryJdbcType extends VarbinaryJdbcType {
	public static final BinaryJdbcType INSTANCE = new BinaryJdbcType();

	public BinaryJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.BINARY;
	}
}
