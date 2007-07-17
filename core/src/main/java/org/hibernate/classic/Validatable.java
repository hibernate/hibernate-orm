//$Id: Validatable.java 4112 2004-07-28 03:33:35Z oneovthafew $
package org.hibernate.classic;


/**
 * Implemented by persistent classes with invariants that must
 * be checked before inserting into or updating the database.
 *
 * @author Gavin King
 */
public interface Validatable {
	/**
	 * Validate the state of the object before persisting it.
	 * If a violation occurs, throw a <tt>ValidationFailure</tt>.
	 * This method must not change the state of the object by
	 * side-effect.
	 * @throws ValidationFailure if an invariant is violated
	 */
	public void validate() throws ValidationFailure;
}






