/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.hql.internal.ast;

/**
 * A custom token class for the HQL grammar.
 * <p><i>NOTE:<i> This class must be public becuase it is instantiated by the ANTLR library.  Ignore any suggestions
 * by various code 'analyzers' about this class being package local.</p>
 */
public class HqlToken extends antlr.CommonToken {
	/**
	 * True if this token could be an identifier.
	 */
	private boolean possibleID;
	/**
	 * The previous token type.
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
	@Override
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
	@Override
	public String toString() {
		return "[\""
				+ getText()
				+ "\",<" + getType() + "> previously: <" + getPreviousType() + ">,line="
				+ line + ",col="
				+ col + ",possibleID=" + possibleID + "]";
	}

}
