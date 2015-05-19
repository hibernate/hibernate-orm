/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.internal.util.StringHelper;

/**
 * A singleton helper class for printing tree structures using indentation.
 *
 * @author Steve Ebersole
 */
public class TreePrinterHelper {
	/**
	 * The number of characters to indent.
	 */
	public static final int INDENTATION = 3;

	/**
	 * Singleton access
	 */
	public static final TreePrinterHelper INSTANCE = new TreePrinterHelper();

	private TreePrinterHelper() {
	}

	/**
	 * Returns a string containing the specified number of indentations, where
	 * each indentation contains {@link #INDENTATION} blank characters.
	 *
	 *
	 * @param nIndentations the number of indentations in the returned String.
	 * @return the String containing the specified number of indentations.
	 */
	public String generateNodePrefix(int nIndentations) {
		return StringHelper.repeat( ' ', nIndentations * INDENTATION ) + " - ";
	}
}
