/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.envers.boot.model.Column;
import org.hibernate.envers.boot.model.ColumnContainer;
import org.hibernate.envers.internal.tools.StringTools;

/**
 * @author Chris Cranford
 */
public class RevisionInfoHelper {

	public static void addOrModifyColumn(ColumnContainer attribute, String name) {
		if ( attribute.getColumns().isEmpty() ) {
			attribute.addColumn( new Column( name ) );
		}
		else {
			if ( !StringTools.isEmpty( name ) ) {
				final Column column = attribute.getColumns().get( 0 );
				column.setName( name );
			}
		}
	}

	private RevisionInfoHelper() {
	}
}
