/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.spi;

import java.sql.SQLException;
import java.util.function.Function;

import org.hibernate.internal.util.NullnessUtil;

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
	public @Nullable String extractConstraintName(SQLException sqle) {
		try {
			String constraintName = null;

			// handle nested exceptions
			do {
				constraintName = extractConstraintName.apply(sqle);
				if (sqle.getNextException() == null
						|| sqle.getNextException() == sqle) {
					break;
				}
				else {
					sqle = NullnessUtil.castNonNull( sqle.getNextException() );
				}
			} while (constraintName == null);

			return constraintName;
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
		int templateStartPosition = message.indexOf( templateStart );
		if ( templateStartPosition < 0 ) {
			return null;
		}

		int start = templateStartPosition + templateStart.length();
		int end = message.indexOf( templateEnd, start );
		if ( end < 0 ) {
			end = message.length();
		}

		return message.substring( start, end );
	}

}
