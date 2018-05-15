/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;


import org.hibernate.NotYetImplementedFor6Exception;

/**
 * @author Rob Worsnop
 */
public class DynamicFilterAliasGenerator implements FilterAliasGenerator {
	private String[] tables;
	private String rootAlias;

	public DynamicFilterAliasGenerator(String[] tables, String rootAlias) {
		this.tables = tables;
		this.rootAlias = rootAlias;
	}

	@Override
	public String getAlias(String table) {
		throw new NotYetImplementedFor6Exception();
//		if ( table == null ) {
//			return rootAlias;
//		}
//		else {
//			return EntityDescriptor.generateTableAlias(
//					rootAlias,
//					AbstractEntityPersister.getTableId( table, tables )
//			);
//		}
	}

}
