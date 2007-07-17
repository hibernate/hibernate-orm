//$Id: ForeignKeyMetadata.java 4279 2004-08-13 08:05:47Z oneovthafew $
package org.hibernate.tool.hbm2ddl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC foreign key metadata
 * @author Christoph Sturm
 */
public class ForeignKeyMetadata {
	private final String name;
	private final List columns = new ArrayList();

	ForeignKeyMetadata(ResultSet rs) throws SQLException {
		name = rs.getString("FK_NAME");
	}

	public String getName() {
		return name;
	}

	void addColumn(ColumnMetadata column) {
		if (column != null) columns.add(column);
	}

	public ColumnMetadata[] getColumns() {
		return (ColumnMetadata[]) columns.toArray(new ColumnMetadata[0]);
	}

	public String toString() {
		return "ForeignKeyMetadata(" + name + ')';
	}
}






