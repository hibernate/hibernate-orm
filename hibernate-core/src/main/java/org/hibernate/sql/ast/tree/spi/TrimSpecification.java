/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public enum TrimSpecification {
	LEADING,
	TRAILING,
	BOTH;

	public String toSqlText() {
		return name().toLowerCase( Locale.ROOT );
	}
}
