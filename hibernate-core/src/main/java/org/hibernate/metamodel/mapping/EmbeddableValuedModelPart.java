/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * An embedded (embeddable-valued) model part.
 *
 * @see javax.persistence.Embedded
 * @see javax.persistence.EmbeddedId
 * @see javax.persistence.Embeddable
 *
 * @author Steve Ebersole
 */
public interface EmbeddableValuedModelPart extends ModelPart, Fetchable, FetchableContainer, TableGroupJoinProducer {
	EmbeddableMappingType getEmbeddableTypeDescriptor();

	/**
	 * The table expression (table name or subselect) that contains
	 * the columns to which this embedded is mapped.
	 *
	 * @apiNote Hibernate has historically required a composite to be mapped to the same table.
	 */
	String getContainingTableExpression();

	/**
	 * The column expressions (column name or formula) to which this embedded value
	 * is mapped
	 */
	List<String> getMappedColumnExpressions();

	/**
	 * @see org.hibernate.annotations.Parent
	 */
	SingularAttributeMapping getParentInjectionAttributeMapping();

	Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState);
}
