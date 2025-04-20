/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

import org.checkerframework.checker.nullness.qual.Nullable;

import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.internal.SqmUtil.determineAffectedTableName;
import static org.hibernate.query.sqm.internal.SqmUtil.getTargetMappingIfNeeded;

/**
 * @author Steve Ebersole
 */
public class BasicValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable,  DomainResultProducer<T> {
	/**
	 * Static factory
	 */
	public static <T> BasicValuedPathInterpretation<T> from(
			SqmBasicValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter sqlAstCreationState,
			boolean jpaQueryComplianceEnabled) {
		final SqlAstCreationContext creationContext = sqlAstCreationState.getCreationContext();

		final SqmPath<?> lhs = sqmPath.getLhs();
		final TableGroup tableGroup =
				sqlAstCreationState.getFromClauseAccess()
						.getTableGroup( lhs.getNavigablePath() );
		final ModelPartContainer tableGroupModelPart = tableGroup.getModelPart();

		final MappingMetamodel mappingMetamodel = creationContext.getMappingMetamodel();
		final EntityMappingType treatTarget;
		final ModelPartContainer modelPartContainer;
		if ( lhs instanceof SqmTreatedPath<?, ?> treatedPath
				&& treatedPath.getTreatTarget().getPersistenceType() == ENTITY ) {
			final EntityDomainType<?> treatTargetDomainType = (EntityDomainType<?>) treatedPath.getTreatTarget();
			final EntityPersister treatEntityDescriptor =
					mappingMetamodel.findEntityDescriptor( treatTargetDomainType.getHibernateEntityName() );
			if ( tableGroupModelPart.getPartMappingType() instanceof EntityMappingType entityMappingType
					&& treatEntityDescriptor.isTypeOrSuperType( entityMappingType ) ) {
				modelPartContainer = tableGroupModelPart;
				treatTarget = treatEntityDescriptor;
			}
			else {
				modelPartContainer = treatEntityDescriptor;
				treatTarget = null;
			}
		}
		else {
			modelPartContainer = tableGroupModelPart;
			treatTarget =
					jpaQueryComplianceEnabled && lhs.getNodeType() instanceof EntityDomainType<?> entityDomainType
							? mappingMetamodel.findEntityDescriptor( entityDomainType.getHibernateEntityName() )
							: null;
		}

		// Use the target type to find the sub part if needed, otherwise just use the container
		final ModelPart modelPart =
				getTargetMappingIfNeeded( sqmPath, modelPartContainer, sqlAstCreationState )
						.findSubPart( sqmPath.getReferencedPathSource().getPathName(), treatTarget );
		if ( modelPart == null ) {
			modelPartError( sqmPath, jpaQueryComplianceEnabled, tableGroup );
		}

		final NavigablePath navigablePath = sqmPath.getNavigablePath();
		final BasicValuedModelPart mapping = castNonNull( modelPart.asBasicValuedModelPart() );
		final TableReference tableReference =
				tableGroup.resolveTableReference( navigablePath, mapping, mapping.getContainingTableExpression() );
		final Expression expression =
				sqlAstCreationState.getSqlExpressionResolver()
						.resolveSqlExpression( tableReference, mapping );
		return new BasicValuedPathInterpretation<>( columnReference( expression ), navigablePath, mapping, tableGroup );
	}

	private static <T> void modelPartError(
			SqmBasicValuedSimplePath<T> sqmPath,
			boolean jpaQueryComplianceEnabled,
			TableGroup tableGroup) {
		if ( jpaQueryComplianceEnabled ) {
			// to get the better error, see if we got nothing because of treat handling
			final ModelPart subPart =
					tableGroup.getModelPart()
							.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );
			if ( subPart != null ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.IMPLICIT_TREAT );
			}
		}

		throw new UnknownPathException( "Path '" + sqmPath.getNavigablePath() + "' did not reference a known model part" );
	}

	private static ColumnReference columnReference(Expression expression) {
		if ( expression instanceof ColumnReference reference ) {
			return reference;
		}
		else if ( expression instanceof SqlSelectionExpression selection ) {
			final Expression selectedExpression = selection.getSelection().getExpression();
			assert selectedExpression instanceof ColumnReference;
			return (ColumnReference) selectedExpression;
		}
		else {
			throw new UnsupportedOperationException( "Unsupported basic-valued path expression : " + expression );
		}
	}

	private final ColumnReference columnReference;
	private final @Nullable String affectedTableName;

	public BasicValuedPathInterpretation(
			ColumnReference columnReference,
			NavigablePath navigablePath,
			BasicValuedModelPart mapping,
			TableGroup tableGroup) {
		this( columnReference, navigablePath, mapping, tableGroup,
				determineAffectedTableName( tableGroup, mapping ) );
	}

	public BasicValuedPathInterpretation(
			ColumnReference columnReference,
			NavigablePath navigablePath,
			BasicValuedModelPart mapping,
			TableGroup tableGroup,
			@Nullable String affectedTableName) {
		super( navigablePath, mapping, tableGroup );
		assert columnReference != null;
		this.columnReference = columnReference;
		this.affectedTableName = affectedTableName;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public Expression getSqlExpression() {
		return columnReference;
	}

	@Override
	public @Nullable String getAffectedTableName() {
		return affectedTableName;
	}

	@Override
	public ColumnReference getColumnReference() {
		return columnReference;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		columnReference.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "BasicValuedPathInterpretation(" + getNavigablePath() + ")";
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		columnReferenceConsumer.accept( columnReference );
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		return Collections.singletonList( columnReference );
	}
}
