/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.audit.ModificationType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.singletonList;

/**
 * HQL function for accessing audit columns of temporal entities.
 * <ul>
 *   <li>{@code transactionId(e)}: returns the transaction identifier
 *   <li>{@code modificationType(e)}: returns the modification type
 * </ul>
 * These functions are only valid for
 * {@linkplain org.hibernate.annotations.Audited audited} entities
 * queried in
 * {@linkplain org.hibernate.audit.AuditLog#ALL_REVISIONS all-revisions}
 * mode.
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class AuditColumnFunction extends AbstractSqmFunctionDescriptor {

	public static final String TRANSACTION_ID_FUNCTION = "transactionId";
	public static final String MODIFICATION_TYPE_FUNCTION = "modificationType";

	private static final FunctionRenderer PASSTHROUGH_RENDERER =
			(sqlAppender, sqlAstArguments, returnType, walker)
					-> sqlAstArguments.get( 0 ).accept( walker );

	private final boolean transactionId;

	public AuditColumnFunction(String name, boolean transactionId, TypeConfiguration typeConfiguration) {
		super(
				name,
				StandardArgumentsValidators.exactly( 1 ),
				transactionId
						// transactionId: type is unknown at registration time, resolved at SQL AST conversion
						? StandardFunctionReturnTypeResolvers.invariant(
						new BasicTypeImpl<>( new UnknownBasicJavaType<>( Object.class ), ObjectJdbcType.INSTANCE )
				)
						// modificationType: proper enum type
						: StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.standardBasicTypeForJavaType( ModificationType.class )
				),
				null
		);
		this.transactionId = transactionId;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new AuditColumnSqmFunction<>(
				this,
				transactionId,
				arguments,
				impliedResultType,
				queryEngine
		);
	}

	private static class AuditColumnSqmFunction<T> extends SelfRenderingSqmFunction<T> {

		private final boolean transactionId;

		AuditColumnSqmFunction(
				AuditColumnFunction descriptor,
				boolean transactionId,
				List<? extends SqmTypedNode<?>> arguments,
				ReturnableType<T> impliedResultType,
				QueryEngine queryEngine) {
			super(
					descriptor,
					PASSTHROUGH_RENDERER,
					arguments,
					impliedResultType,
					descriptor.getArgumentsValidator(),
					descriptor.getReturnTypeResolver(),
					queryEngine.getCriteriaBuilder(),
					descriptor.getName()
			);
			this.transactionId = transactionId;
		}

		private AuditColumnSqmFunction(
				SqmFunctionDescriptor descriptor,
				FunctionRenderer renderer,
				boolean transactionId,
				List<? extends SqmTypedNode<?>> arguments,
				ReturnableType<T> impliedResultType,
				ArgumentsValidator argumentsValidator,
				FunctionReturnTypeResolver returnTypeResolver,
				NodeBuilder nodeBuilder,
				String name) {
			super(
					descriptor, renderer, arguments, impliedResultType,
					argumentsValidator, returnTypeResolver, nodeBuilder, name
			);
			this.transactionId = transactionId;
		}

		@Override
		public AuditColumnSqmFunction<T> copy(SqmCopyContext context) {
			final var existing = context.getCopy( this );
			if ( existing != null ) {
				return existing;
			}
			final var arguments = new ArrayList<SqmTypedNode<?>>( getArguments().size() );
			for ( var argument : getArguments() ) {
				arguments.add( argument.copy( context ) );
			}
			return context.registerCopy(
					this,
					new AuditColumnSqmFunction<>(
							getFunctionDescriptor(),
							getFunctionRenderer(),
							transactionId,
							arguments,
							getImpliedResultType(),
							getArgumentsValidator(),
							getReturnTypeResolver(),
							nodeBuilder(),
							getFunctionName()
					)
			);
		}

		@Override
		public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
			final var entityPath = (SqmPath<?>) getArguments().get( 0 );

			final var tableGroup = walker.getFromClauseAccess()
					.findTableGroup( entityPath.getNavigablePath() );

			final var entityMapping = (EntityMappingType) tableGroup.getModelPart();
			final var auditMapping = entityMapping.getAuditMapping();
			if ( auditMapping == null ) {
				throw new IllegalArgumentException(
						"Entity '" + entityMapping.getEntityName()
								+ "' is not audited"
				);
			}

			// modificationType lives on the root (identifier) table, not subclass tables
			final String originalTable = transactionId
					? entityMapping.getMappedTableDetails().getTableName()
					: entityMapping.getIdentifierTableDetails().getTableName();
			final SelectableMapping selectableMapping = transactionId
					? auditMapping.getTransactionIdMapping( originalTable )
					: auditMapping.getModificationTypeMapping( originalTable );

			final var tableReference = tableGroup.resolveTableReference(
					entityPath.getNavigablePath(),
					originalTable
			);

			final var columnReference =
					new ColumnReference( tableReference, selectableMapping );
			return new SelfRenderingFunctionSqlAstExpression<>(
					getFunctionName(),
					getFunctionRenderer(),
					singletonList( columnReference ),
					null,
					selectableMapping.getJdbcMapping()
			);
		}
	}
}
