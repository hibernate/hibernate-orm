/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;

import org.hibernate.internal.util.StringHelper;

/**
 * Generates class/table/column aliases during semantic analysis and SQL rendering.
 * <p/>
 * Its essential purpose is to keep an internal counter to ensure that the
 * generated aliases are unique.
 */
public class AliasGenerator {
	private int next;

	private int nextCount() {
		return next++;
	}

	public String createName(String name) {
		return StringHelper.generateAlias( name, nextCount() );
	}
}
