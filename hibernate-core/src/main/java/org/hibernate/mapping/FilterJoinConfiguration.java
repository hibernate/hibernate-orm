/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;

import java.util.Arrays;

/**
 * Join metadata for applying a filter condition on a joined table.
 */
@Incubating
public record FilterJoinConfiguration
		(String tableName, String[] joinColumnNames, String[] referencedColumnNames) {

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof FilterJoinConfiguration that ) ) {
			return false;
		}
		else {
			return tableName.equals( that.tableName )
				&& Arrays.equals( joinColumnNames, that.joinColumnNames )
				&& Arrays.equals( referencedColumnNames, that.referencedColumnNames );
		}
	}

	@Override
	public int hashCode() {
		int result = tableName.hashCode();
		result = 31 * result + Arrays.hashCode( joinColumnNames );
		result = 31 * result + Arrays.hashCode( referencedColumnNames );
		return result;
	}
}
