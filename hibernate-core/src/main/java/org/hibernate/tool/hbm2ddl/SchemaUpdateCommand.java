/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

/**
 * Pairs a SchemaUpdate SQL script with the boolean 'quiet'.  If true, it allows
 * the script to be run, ignoring all exceptions.
 *
 * @author Brett Meyer
 * @author Steve Ebersole
 */
public class SchemaUpdateCommand {
	private final String sql;
	private final boolean quiet;

	public SchemaUpdateCommand(String sql, boolean quiet) {
		this.sql = sql;
		this.quiet = quiet;
	}

	public String getSql() {
		return sql;
	}

	public boolean isQuiet() {
		return quiet;
	}
}
