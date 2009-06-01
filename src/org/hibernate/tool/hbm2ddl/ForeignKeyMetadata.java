//$Id$
package org.hibernate.tool.hbm2ddl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;

/**
 * JDBC foreign key metadata
 * @author Christoph Sturm
 */
public class ForeignKeyMetadata {
	private final String name;
	private final String refTable;
	private final Map references = new HashMap();

	ForeignKeyMetadata(ResultSet rs) throws SQLException {
		name = rs.getString("FK_NAME");
		refTable = rs.getString("PKTABLE_NAME");
	}

	public String getName() {
		return name;
	}

	void addReference(ResultSet rs) throws SQLException {
		references.put(rs.getString("FKCOLUMN_NAME").toLowerCase(), 
				rs.getString("PKCOLUMN_NAME"));
	}

	private boolean hasReference(Column column, Column ref) {
		String refName = (String) references.get(column.getName().toLowerCase());
		return ref.getName().equalsIgnoreCase(refName);
	}

	public boolean matches(ForeignKey fk) {
		if (refTable.equalsIgnoreCase(fk.getReferencedTable().getName())) {
			if (fk.getColumnSpan() == references.size()) {
				List fkRefs;
				if (fk.isReferenceToPrimaryKey()) {
					fkRefs = fk.getReferencedTable().getPrimaryKey().getColumns();
				}
				else {
					fkRefs = fk.getReferencedColumns();				
				}
				for (int i = 0; i < fk.getColumnSpan(); i++) {
					Column column = fk.getColumn(i);
					Column ref = (Column) fkRefs.get(i);
					if (!hasReference(column, ref)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return "ForeignKeyMetadata(" + name + ')';
	}
}






