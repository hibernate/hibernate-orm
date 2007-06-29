// $Id: TemplatedViolatedConstraintNameExtracter.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.exception;



/**
 * Knows how to extract a violated constraint name from an error message based on the
 * fact that the constraint name is templated within the message.
 *
 * @author Steve Ebersole
 */
public abstract class TemplatedViolatedConstraintNameExtracter implements ViolatedConstraintNameExtracter {

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
