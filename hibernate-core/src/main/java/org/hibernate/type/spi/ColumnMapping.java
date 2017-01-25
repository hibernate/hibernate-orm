/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Models the aspect of mapping a Java type (or an individual part of a Java type) to a database column
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public class ColumnMapping {
	// todo : better name
	//		ColumnDefinition?

	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final Size dictatedSize;
	private final Size defaultSize;

	public ColumnMapping(SqlTypeDescriptor sqlTypeDescriptor) {
		this( sqlTypeDescriptor, null, null );
	}

	public ColumnMapping(SqlTypeDescriptor sqlTypeDescriptor, Size dictatedSize, Size defaultSize) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.dictatedSize = dictatedSize;
		this.defaultSize = defaultSize;
	}

	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	public Size getDictatedSize() {
		return dictatedSize;
	}

	public Size getDefaultSize() {
		return defaultSize;
	}
}
