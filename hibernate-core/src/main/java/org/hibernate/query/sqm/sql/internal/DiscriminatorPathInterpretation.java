/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.spi.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.EmbeddedDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.EntityDiscriminatorSqmPath;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import static org.hibernate.query.sqm.internal.SqmMappingModelHelper.resolveMappingModelExpressible;

/**
 * SqmPathInterpretation and DomainResultProducer implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class DiscriminatorPathInterpretation<T> extends AbstractSqmPathInterpretation<T> {
	private final Expression expression;

	public DiscriminatorPathInterpretation(
			NavigablePath navigablePath,
			DiscriminatorMapping mapping,
			TableGroup tableGroup,
			SqlAstCreationState sqlAstCreationState) {
		super( navigablePath, mapping, tableGroup );
		final var jdbcMappingToUse = mapping.getJdbcMapping();
		expression = getDiscriminatorMapping()
				.resolveSqlExpression( navigablePath, jdbcMappingToUse, tableGroup, sqlAstCreationState );
	}

	public DiscriminatorPathInterpretation(
			NavigablePath navigablePath,
			EntityMappingType mapping,
			TableGroup tableGroup,
			SqlAstCreationState sqlAstCreationState) {
		super( navigablePath, mapping.getDiscriminatorMapping(), tableGroup );
		final var jdbcMappingToUse = mapping.getDiscriminatorMapping().getJdbcMapping();
		expression = getDiscriminatorMapping()
				.resolveSqlExpression( navigablePath, jdbcMappingToUse, tableGroup, sqlAstCreationState );
	}

	public static SqmPathInterpretation<?> from(
			DiscriminatorSqmPath<?> path,
			SqmToSqlAstConverter converter) {

		final var navigablePath = path.getNavigablePath();
		final var tableGroup = converter.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
		final var modelPart = tableGroup.getModelPart();

		if ( path instanceof EntityDiscriminatorSqmPath<?> entityDiscriminatorSqmPath ) {
			assert entityDiscriminatorSqmPath.getEntityDescriptor().hasSubclasses();
			final var entityMapping = entityMappingType( modelPart );
			return new DiscriminatorPathInterpretation<>( navigablePath, entityMapping, tableGroup, converter );
		}
		else if ( path instanceof EmbeddedDiscriminatorSqmPath<?> embeddedDiscriminatorSqmPath ) {
			return new DiscriminatorPathInterpretation<>(
					navigablePath,
					(DiscriminatorMapping)
							resolveMappingModelExpressible(
									embeddedDiscriminatorSqmPath,
									converter.getCreationContext().getMappingMetamodel(),
									converter.getFromClauseAccess()::findTableGroup
							),
					tableGroup,
					converter
			);
		}
		else {
			throw new AssertionFailure( "Unrecognized path type" );
		}
	}

	private static EntityMappingType entityMappingType(ModelPartContainer modelPart) {
		if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			return entityValuedModelPart.getEntityMappingType();
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping) {
			return  (EntityMappingType) pluralAttributeMapping.getElementDescriptor().getPartMappingType();
		}
		else {
			throw new AssertionFailure( "Unrecognized model part type" );
		}
	}

	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return (EntityDiscriminatorMapping) super.getExpressionType();
	}

	@Override
	public EntityDiscriminatorMapping getExpressionType() {
		return getDiscriminatorMapping();
	}

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return getDiscriminatorMapping().createDomainResult( getNavigablePath(), getTableGroup(), resultVariable, creationState );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		expression.accept( sqlTreeWalker );
	}
}
