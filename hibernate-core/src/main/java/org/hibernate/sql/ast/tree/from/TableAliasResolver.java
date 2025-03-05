/*
 * SPDX-License-Identifier: Apache-2.0
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
