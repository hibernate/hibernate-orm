/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.persister.entity.AbstractEntityPersister;

/**
 * @author Rob Worsnop
 */
public class DynamicFilterAliasGenerator implements FilterAliasGenerator {
	private final String[] tables;
	private final String rootAlias;

	public DynamicFilterAliasGenerator(String[] tables, String rootAlias) {
		this.tables = tables;
		this.rootAlias = rootAlias;
	}

	@Override
	public String getAlias(String table) {
		if ( table == null ) {
			return rootAlias;
		}
		else {
			return AbstractEntityPersister.generateTableAlias(
					rootAlias,
					AbstractEntityPersister.getTableId( table, tables )
			);
		}
	}

}
