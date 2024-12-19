/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignable;

import org.checkerframework.checker.nullness.qual.Nullable;

import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static org.hibernate.query.sqm.internal.SqmUtil.getTargetMappingIfNeeded;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable, SqlTupleContainer {

	/**
	 * Static factory
	 */
	public static <T> Expression from(
			SqmEmbeddedValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter sqlAstCreationState,
			boolean jpaQueryComplianceEnabled) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( lhs.getNavigablePath() );
		EntityMappingType treatTarget = null;
		if ( jpaQueryComplianceEnabled ) {
			final MappingMetamodel mappingMetamodel = sqlAstCreationState.getCreationContext()
					.getSessionFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel();
			if ( lhs instanceof SqmTreatedPath<?, ?> && ( (SqmTreatedPath<?, ?>) lhs ).getTreatTarget().getPersistenceType() == ENTITY ) {
				final EntityDomainType<?> treatTargetDomainType = (EntityDomainType<?>) ( (SqmTreatedPath<?, ?>) lhs ).getTreatTarget();
				treatTarget = mappingMetamodel.findEntityDescriptor( treatTargetDomainType.getHibernateEntityName() );
			}
			else if ( lhs.getNodeType() instanceof EntityDomainType ) {
				//noinspection rawtypes
				final EntityDomainType<?> entityDomainType = (EntityDomainType) lhs.getNodeType();
				treatTarget = mappingMetamodel.findEntityDescriptor( entityDomainType.getHibernateEntityName() );
			}
		}

		final ModelPartContainer modelPartContainer = tableGroup.getModelPart();
		// Use the target type to find the sub part if needed, otherwise just use the container
		final EmbeddableValuedModelPart mapping = (EmbeddableValuedModelPart) getTargetMappingIfNeeded(
				sqmPath,
				modelPartContainer,
				sqlAstCreationState
		).findSubPart( sqmPath.getReferencedPathSource().getPathName(), treatTarget );

		return new EmbeddableValuedPathInterpretation<>(
				mapping.toSqlExpression(
						tableGroup,
						sqlAstCreationState.getCurrentClauseStack().getCurrent(),
						sqlAstCreationState,
						sqlAstCreationState
				),
				sqmPath.getNavigablePath(),
				mapping,
				tableGroup
		);
	}

	private final SqlTuple sqlExpression;
	private final @Nullable String affectedTableName;

	public EmbeddableValuedPathInterpretation(
			SqlTuple sqlExpression,
			NavigablePath navigablePath,
			EmbeddableValuedModelPart mapping,
			TableGroup tableGroup) {
		this( sqlExpression, navigablePath, mapping, tableGroup, determineAffectedTableName( tableGroup, mapping ) );
	}

	public EmbeddableValuedPathInterpretation(
				SqlTuple sqlExpression,
				NavigablePath navigablePath,
				EmbeddableValuedModelPart mapping,
				TableGroup tableGroup,
				@Nullable String affectedTableName) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
		this.affectedTableName = affectedTableName;
	}

	private static @Nullable String determineAffectedTableName(TableGroup tableGroup, EmbeddableValuedModelPart mapping) {
		final ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) modelPart;
			if ( !associationMapping.containsTableReference( mapping.getContainingTableExpression() ) ) {
				return associationMapping.getAssociatedEntityMappingType().getMappedTableDetails().getTableName();
			}
		}
		return null;
	}

	@Override
	public SqlTuple getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public @Nullable String getAffectedTableName() {
		return affectedTableName;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		if ( affectedTableName != null && sqlTreeWalker instanceof SqlAstTranslator<?> ) {
			( (SqlAstTranslator<?>) sqlTreeWalker ).addAffectedTableName( affectedTableName );
		}
		sqlExpression.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "EmbeddableValuedPathInterpretation(" + getNavigablePath() + ")";
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		for ( Expression expression : sqlExpression.getExpressions() ) {
			if ( !( expression instanceof ColumnReference ) ) {
				throw new IllegalArgumentException( "Expecting ColumnReference, found : " + expression );
			}
			columnReferenceConsumer.accept( (ColumnReference) expression );
		}
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		final List<ColumnReference> results = new ArrayList<>();
		visitColumnReferences( results::add );
		return results;
	}

	@Override
	public SqlTuple getSqlTuple() {
		return sqlExpression;
	}
}
