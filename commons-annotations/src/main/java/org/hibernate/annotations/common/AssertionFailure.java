//$Id: $
package org.hibernate.annotations.common;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * Indicates failure of an assertion: a possible bug in Hibernate.
 *
 * @author Gavin King
 * @auhor Emmanuel Bernard
 */
//TODO Copy from Hibernate Core, do some mutualization here?
public class AssertionFailure extends RuntimeException {

	private static final Logger log = LoggerFactory.getLogger(AssertionFailure.class);

	private static final String MESSAGE = "an assertion failure occured (this may indicate a bug in Hibernate)";

	public AssertionFailure(String s) {
		super(s);
		log.error(MESSAGE, this);
	}

	public AssertionFailure(String s, Throwable t) {
		super(s, t);
		log.error(MESSAGE, t);
	}

}
