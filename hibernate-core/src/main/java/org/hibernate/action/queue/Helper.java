/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.persister.entity.EntityPersister;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class Helper {
	// Keep normalization consistent with your FK builder/tests (optional)
	public static String normalizeTableName(String table) {
		if (table == null) return "";
		return table.toLowerCase( Locale.ROOT)
				.replace("\"", "")
				.replace("`", "")
				.replace("[", "")
				.replace("]", "")
				.trim();
	}

	public static String normalizeColumnName(String s) {
		if ( s == null ) return "";
		String x = s.trim();
		int dot = x.lastIndexOf( '.' );
		if ( dot >= 0 ) x = x.substring( dot + 1 );
		return x.toLowerCase( Locale.ROOT ).replace( "\"", "" ).replace( "`", "" );
	}

	public static boolean needsIdentityPrePhase(EntityPersister persister, Object identifier) {
		// IDENTITY generation needs pre-phase execution when ID is not yet assigned
		// (i.e., identifier == null means database will generate it)
		return persister.getGenerator().generatedOnExecution() && identifier == null;
	}
}
