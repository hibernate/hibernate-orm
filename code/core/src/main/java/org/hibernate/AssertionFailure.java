//$Id: AssertionFailure.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate;

import org.hibernate.exception.NestableRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Indicates failure of an assertion: a possible bug in Hibernate.
 *
 * @author Gavin King
 */

public class AssertionFailure extends NestableRuntimeException {

	private static final Log log = LogFactory.getLog(AssertionFailure.class);

	private static final String MESSAGE = "an assertion failure occured (this may indicate a bug in Hibernate, but is more likely due to unsafe use of the session)";

	public AssertionFailure(String s) {
		super(s);
		log.error(MESSAGE, this);
	}

	public AssertionFailure(String s, Throwable t) {
		super(s, t);
		log.error(MESSAGE, t);
	}

}






