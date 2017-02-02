/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.spi;

import java.sql.SQLException;

/**
 * Knows how to extract a violated constraint name from an error message based on the
 * fact that the constraint name is templated within the message.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class TemplatedViolatedConstraintNameExtracter implements ViolatedConstraintNameExtracter {

	@Override
	public String extractConstraintName(SQLException sqle) {
		try {
			String constraintName = null;

			// handle nested exceptions
			do {
				constraintName = doExtractConstraintName(sqle);
				if (sqle.getNextException() == null
						|| sqle.getNextException() == sqle) {
					break;
				}
				else {
					sqle = sqle.getNextException();
				}
			} while (constraintName == null);

			return constraintName;
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}

	protected abstract String doExtractConstraintName(SQLException sqle) throws NumberFormatException;

	/**
	 * Extracts the constraint name based on a template (i.e., <i>templateStart</i><b>constraintName</b><i>templateEnd</i>).
	 *
	 * @param templateStart The pattern denoting the start of the constraint name within the message.
	 * @param templateEnd   The pattern denoting the end of the constraint name within the message.
	 * @param message       The templated error message containing the constraint name.
	 * @return The found constraint name, or null.
	 */
	protected String extractUsingTemplate(String templateStart, String templateEnd, String message) {
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
