package org.hibernate.dialect.type.internal;

import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

/**
 * @author Christian Beikov
 */
public class MySQLVarbinaryJdbcType extends VarbinaryJdbcType {
	/**
	 * Singleton access
	 */
	public static final MySQLVarbinaryJdbcType INSTANCE = new MySQLVarbinaryJdbcType();

	public MySQLVarbinaryJdbcType() {
		super();
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping, @Nullable Size size) {
		if ( size != null && size.getLength() != null ) {
			return "cast(substr(cast(?1 as binary),1," + size.getLength() + ") as binary)";
		}
		else {
			return null;
		}
	}
}
