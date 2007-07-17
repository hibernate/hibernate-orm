// $Id: HqlToken.java 4335 2004-08-15 17:03:43Z pgmjsd $
package org.hibernate.hql.ast;

/**
 * A custom token class for the HQL grammar.
 * <p><i>NOTE:<i> This class must be public becuase it is instantiated by the ANTLR library.  Ignore any suggestions
 * by various code 'analyzers' about this class being package local.</p>
 */
public class HqlToken extends antlr.CommonToken {
	/**
	 * True if this token could be an identifier. *
	 */
	private boolean possibleID = false;
	/**
	 * The previous token type. *
	 */
	private int tokenType;

	/**
	 * Returns true if the token could be an identifier.
	 *
	 * @return True if the token could be interpreted as in identifier,
	 *         false if not.
	 */
	public boolean isPossibleID() {
		return possibleID;
	}

	/**
	 * Sets the type of the token, remembering the previous type.
	 *
	 * @param t The new token type.
	 */
	public void setType(int t) {
		this.tokenType = getType();
		super.setType( t );
	}

	/**
	 * Returns the previous token type.
	 *
	 * @return int - The old token type.
	 */
	private int getPreviousType() {
		return tokenType;
	}

	/**
	 * Set to true if this token can be interpreted as an identifier,
	 * false if not.
	 *
	 * @param possibleID True if this is a keyword/identifier, false if not.
	 */
	public void setPossibleID(boolean possibleID) {
		this.possibleID = possibleID;
	}

	/**
	 * Returns a string representation of the object.
	 *
	 * @return String - The debug string.
	 */
	public String toString() {
		return "[\""
				+ getText()
				+ "\",<" + getType() + "> previously: <" + getPreviousType() + ">,line="
				+ line + ",col="
				+ col + ",possibleID=" + possibleID + "]";
	}

}
