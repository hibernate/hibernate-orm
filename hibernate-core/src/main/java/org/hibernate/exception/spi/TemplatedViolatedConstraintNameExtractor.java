/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.spi;

import java.sql.SQLException;
import java.util.function.Function;


import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Extracts a violated database constraint name from an error message
 * by matching the error message against a template.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class TemplatedViolatedConstraintNameExtractor implements ViolatedConstraintNameExtractor {

	private final Function<SQLException,String> extractConstraintName;

	public TemplatedViolatedConstraintNameExtractor(Function<SQLException,String> extractConstraintName) {
		this.extractConstraintName = extractConstraintName;
	}

	@Override
	public @Nullable String extractConstraintName(SQLException exception) {
		try {
			while (true) {
				final String constraintName = extractConstraintName.apply( exception );
				final SQLException chained = exception.getNextException();
				if ( constraintName != null
						|| chained == null
						|| chained == exception ) {
					return constraintName;
				}
				exception = chained;
			}
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}

	/**
	 * Extracts the constraint name based on a template of form
	 * <i>templateStart</i><b>constraintName</b><i>templateEnd</i>.
	 *
	 * @param templateStart The pattern denoting the start of the constraint name within the message.
	 * @param templateEnd   The pattern denoting the end of the constraint name within the message.
	 * @param message       The templated error message containing the constraint name.
	 * @return The found constraint name, or null.
	 */
	public static @Nullable String extractUsingTemplate(String templateStart, String templateEnd, String message) {
		final int templateStartPosition = message.indexOf( templateStart );
		if ( templateStartPosition < 0 ) {
			return null;
		}
		else {
			final int start = templateStartPosition + templateStart.length();
			final int end = templateEnd.equals( "\n" ) ? -1 : message.indexOf( templateEnd, start );
			return end < 0 ? message.substring( start ) : message.substring( start, end );
		}
	}

}
