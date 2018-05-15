/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;


import org.hibernate.annotations.Remove;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 *
 * @deprecated Converting to use SQL AST - Integrated into {@link org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator}
 */
@Deprecated
@Remove
public interface AliasGenerator {
	public String generateAlias(String sqlExpression);
}
