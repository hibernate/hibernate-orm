/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class Helper {
	// Keep normalization consistent with your FK builder/tests (optional)
	public static String normalizeTableName(String table) {
		if (table == null) {
			return "";
		}

//		return table.toLowerCase( Locale.ROOT)
//				.replace("\"", "")
//				.replace("`", "")
//				.replace("[", "")
//				.replace("]", "")
//				.trim();

		table = table.trim();

		if ( Identifier.isQuoted( table ) ) {
			// unquote?
			return table;
		}

		return table.toLowerCase(Locale.ROOT);
	}

	public static String normalizeColumnName(String name) {
		if ( name == null ) {
			return "";
		}

//		String x = name.trim();
//		int dot = x.lastIndexOf( '.' );
//		if ( dot >= 0 ) x = x.substring( dot + 1 );
//		return x.toLowerCase( Locale.ROOT ).replace( "\"", "" ).replace( "`", "" );

		name = name.trim();
		int dot = name.lastIndexOf( '.' );
		if ( dot >= 0 ) {
			name = name.substring( dot + 1 );
		}

		if ( Identifier.isQuoted( name )  ) {
			// unquote?
			return name;
		}

		return name.toLowerCase(Locale.ROOT);
	}

	public static boolean needsIdentityPrePhase(EntityPersister persister, Object identifier) {
		// IDENTITY generation needs pre-phase execution when ID is not yet assigned
		// (i.e., identifier == null means database will generate it)
		return persister.getGenerator().generatedOnExecution() && identifier == null;
	}
}
