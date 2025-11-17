/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
