/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

/**
 * Handles generating implicit (or synthetic) aliases.
 *
 * @author Steve Ebersole
 */
public class ImplicitAliasGenerator {
	private int unAliasedCount = 0;

	/**
	 * Builds a unique implicit alias.
	 *
	 * @return The generated alias.
	 */
	public synchronized String generateUniqueImplicitAlias() {
		return "<gen:" + unAliasedCount++ + ">";
	}

	/**
	 * Determine if the given alias is implicit.
	 *
	 * @param alias The alias to check
	 * @return True/false.
	 */
	public static boolean isImplicitAlias(String alias) {
		return alias == null || ( alias.startsWith( "<gen:" ) && alias.endsWith( ">" ) );
	}
}
