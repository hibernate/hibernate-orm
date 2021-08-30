/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SqmPathInterpretation and DomainResultProducer implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class DiscriminatorPathInterpretation extends AbstractSqmPathInterpretation implements DomainResultProducer {
	private final Expression expression;

	public DiscriminatorPathInterpretation(
			NavigablePath navigablePath,
			EntityMappingType mapping,
			TableGroup tableGroup,
			SqlAstCreationState sqlAstCreationState) {
		super( navigablePath, mapping.getDiscriminatorMapping(), tableGroup );

		final JdbcMapping jdbcMappingToUse = mapping.getDiscriminatorMapping().getJdbcMapping();
		expression = getDiscriminatorMapping().resolveSqlExpression( navigablePath, jdbcMappingToUse, tableGroup, sqlAstCreationState );
	}

	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return (EntityDiscriminatorMapping) super.getExpressionType();
	}

	@Override
	public EntityDiscriminatorMapping getExpressionType() {
		return getDiscriminatorMapping();
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl( valuesArrayPosition, getDiscriminatorMapping() );
	}

	@Override
	public DomainResult<Class<?>> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return getDiscriminatorMapping().createDomainResult( getNavigablePath(), getTableGroup(), resultVariable, creationState );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		expression.accept( sqlTreeWalker );
	}
}
