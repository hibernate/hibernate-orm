/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;

/**
 * JDBC foreign key metadata
 *
 * @author Christoph Sturm
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
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

	public String getReferencedTableName() {
		return refTable;
	}

	void addReference(ResultSet rs) throws SQLException {
		references.put( rs.getString("FKCOLUMN_NAME").toLowerCase(Locale.ROOT), rs.getString("PKCOLUMN_NAME") );
	}

	public boolean matches(ForeignKey fk) {
		if ( refTable.equalsIgnoreCase( ( (ExportableTable) fk.getReferringTable() ).getTableName().getText() ) ) {
			if ( fk.getColumnMappings().getColumnMappings().size() == references.size() ) {
				List fkRefs;
				if ( fk.isReferenceToPrimaryKey() ) {
					fkRefs = fk.getTargetTable().getPrimaryKey().getColumns();
				}
				else {
					fkRefs = fk.getColumnMappings().getTargetColumns();
				}
				int i = 0;
				for ( Column column : fk.getColumnMappings().getReferringColumns() ) {
					Column ref = (Column) fkRefs.get( i );
					if ( ! column.equals( ref ) ) {
						return false;
					}
					i++;
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
