/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

/**
 * Describes the types of targets for create, drop and update actions
 *
 * @author Steve Ebersole
 */
public enum Target {
	/**
	 * Export to the database.
	 */
	EXPORT,
	/**
	 * Write to a script file.
	 */
	SCRIPT,
	/**
	 * export nowhere
	 */
	NONE,
	/**
	 * Do both {@link #EXPORT} and {@link #SCRIPT}
	 */
	BOTH;

	public boolean doExport() {
		return this == BOTH || this == EXPORT;
	}

	public boolean doScript() {
		return this == BOTH || this == SCRIPT;
	}

	public static Target interpret(boolean script, boolean export) {
		if ( script ) {
			return export ? BOTH : SCRIPT;
		}
		else {
			return export ? EXPORT : NONE;
		}
	}
}
