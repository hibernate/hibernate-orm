/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface TableAliasResolver {
	String resolveAlias(String tableExpression, String aliasStem);
}
