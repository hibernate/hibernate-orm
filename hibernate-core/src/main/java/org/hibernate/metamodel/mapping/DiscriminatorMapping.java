/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * Mapping of a discriminator, either for {@linkplain EntityMappingType#getDiscriminatorMapping() entity}- or
 * {@linkplain DiscriminatedAssociationModelPart#getDiscriminatorPart() any}-based discriminators
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorMapping extends VirtualModelPart, BasicValuedModelPart, Fetchable {
	/**
	 * Information about the value mappings
	 */
	DiscriminatorConverter<?,?> getValueConverter();

	/**
	 * Create the appropriate SQL expression for this discriminator
	 *
	 * @param jdbcMappingToUse The JDBC mapping to use.  This allows opting between
	 * the "domain result type" (aka Class) and the "underlying type" (Integer, String, etc)
	 */
	Expression resolveSqlExpression(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			SqlAstCreationState creationState);
}
