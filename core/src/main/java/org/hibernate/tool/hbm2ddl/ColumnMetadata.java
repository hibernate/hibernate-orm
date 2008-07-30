/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * JDBC column metadata
 * @author Christoph Sturm
 */
public class ColumnMetadata {
	private final String name;
	private final String typeName;
	private final int columnSize;
	private final int decimalDigits;
	private final String isNullable;
	private final int typeCode;

	ColumnMetadata(ResultSet rs) throws SQLException {
		name = rs.getString("COLUMN_NAME");
		columnSize = rs.getInt("COLUMN_SIZE");
		decimalDigits = rs.getInt("DECIMAL_DIGITS");
		isNullable = rs.getString("IS_NULLABLE");
		typeCode = rs.getInt("DATA_TYPE");
		typeName = new StringTokenizer( rs.getString("TYPE_NAME"), "() " ).nextToken();
	}

	public String getName() {
		return name;
	}

	public String getTypeName() {
		return typeName;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public String getNullable() {
		return isNullable;
	}

	public String toString() {
		return "ColumnMetadata(" + name + ')';
	}

	public int getTypeCode() {
		return typeCode;
	}

}






