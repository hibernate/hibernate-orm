//$Id: ColumnMetadata.java 7854 2005-08-11 20:41:21Z oneovthafew $
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






