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
 */
package org.hibernate.tool.hbm2ddl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;

/**
 * JDBC foreign key metadata
 *
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

	public String getReferencedTableName() {
		return refTable;
	}

	void addReference(ResultSet rs) throws SQLException {
		references.put( StringHelper.toLowerCase(rs.getString("FKCOLUMN_NAME")), rs.getString("PKCOLUMN_NAME") );
	}

	private boolean hasReference(Column column, Column ref) {
		String refName = (String) references.get(StringHelper.toLowerCase(column.getName()));
		return ref.getName().equalsIgnoreCase(refName);
	}

	public boolean matches(ForeignKey fk) {
		if ( refTable.equalsIgnoreCase( fk.getReferencedTable().getName() ) ) {
			if ( fk.getColumnSpan() == references.size() ) {
				List fkRefs;
				if ( fk.isReferenceToPrimaryKey() ) {
					fkRefs = fk.getReferencedTable().getPrimaryKey().getColumns();
				}
				else {
					fkRefs = fk.getReferencedColumns();
				}
				for ( int i = 0; i < fk.getColumnSpan(); i++ ) {
					Column column = fk.getColumn( i );
					Column ref = ( Column ) fkRefs.get( i );
					if ( !hasReference( column, ref ) ) {
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






