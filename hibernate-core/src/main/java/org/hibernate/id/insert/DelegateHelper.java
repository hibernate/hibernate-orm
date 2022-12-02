/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.id.PostInsertIdentityPersister;

class DelegateHelper {
	static String getKeyColumnName(PostInsertIdentityPersister persister) {
		String[] columnNames = persister.getRootTableKeyColumnNames();
		if ( columnNames.length != 1 ) {
			//TODO: remove this limitation
			throw new NotYetImplementedFor6Exception("GetGeneratedKeysDelegate does not yet support multi-column Generators");
		}
		return columnNames[0];
	}


}
