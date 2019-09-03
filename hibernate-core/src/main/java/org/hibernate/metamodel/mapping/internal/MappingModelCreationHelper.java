/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.ScalarDomainResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class MappingModelCreationHelper {
	/**
	 * A factory - disallow direct instantiation
	 */
	private MappingModelCreationHelper() {
	}

	public static EntityIdentifierMapping buildSimpleIdentifierMapping(
			EntityPersister entityPersister,
			String rootTable,
			String pkColumnName,
			BasicType idType,
			MappingModelCreationProcess creationProcess) {
		return new EntityIdentifierMapping() {
			@Override
			public MappingType getMappedTypeDescriptor() {
				return ( (BasicType) entityPersister.getIdentifierType() ).getMappedTypeDescriptor();
			}

			@Override
			public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
				return 1;
			}

			@Override
			public void visitJdbcTypes(
					Consumer<JdbcMapping> action,
					Clause clause,
					TypeConfiguration typeConfiguration) {
				action.accept( idType );
			}

			@Override
			public void visitJdbcValues(
					Object value,
					Clause clause,
					JdbcValuesConsumer valuesConsumer,
					SharedSessionContractImplementor session) {
				valuesConsumer.consume( value, idType );
			}

			@Override
			public <T> DomainResult<T> createDomainResult(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					String resultVariable,
					DomainResultCreationState creationState) {
				final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

				final Expression expression = expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( rootTable, pkColumnName ),
						sqlAstProcessingState -> new ColumnReference(
								pkColumnName,
								rootTable,
								( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getJdbcMapping(),
								creationProcess.getCreationContext().getSessionFactory()
						)
				);

				final SqlSelection sqlSelection = expressionResolver.resolveSqlSelection(
						expression,
						idType.getExpressableJavaTypeDescriptor(),
						creationProcess.getCreationContext().getSessionFactory().getTypeConfiguration()
				);

				//noinspection unchecked
				return new ScalarDomainResultImpl(
						sqlSelection.getValuesArrayPosition(),
						resultVariable,
						entityPersister.getIdentifierMapping().getMappedTypeDescriptor().getMappedJavaTypeDescriptor()
				);
			}

			@Override
			public void applySqlSelections(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					DomainResultCreationState creationState) {
				final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

				// todo (6.0) : in the original 6.0 work `#resolveSqlExpression` worked based on an overload to handle qualifiable versus un-qualifiable expressables.
				//		- that gets awkward in terms of managing which overloaded form to call.  Perhaps a better
				//		option would be to use heterogeneous keys - e.g. an array for a qualifiable expressable (alias + expressable)
				//		or a String concatenation

				final Expression expression = expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( rootTable, pkColumnName ),
						sqlAstProcessingState -> new ColumnReference(
								pkColumnName,
								rootTable,
								( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getJdbcMapping(),
								creationProcess.getCreationContext().getSessionFactory()
						)
				);

				// the act of resolving the expression -> selection applies it
				expressionResolver.resolveSqlSelection(
						expression,
						idType.getExpressableJavaTypeDescriptor(),
						creationProcess.getCreationContext().getSessionFactory().getTypeConfiguration()
				);
			}

		};
	}

	public static EntityIdentifierMapping buildEncapsulatedCompositeIdentifierMapping(EntityPersister entityPersister) {
		return new EntityIdentifierMapping() {
			@Override
			public MappingType getMappedTypeDescriptor() {
				return ( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getMappedTypeDescriptor();
			}

			@Override
			public <T> DomainResult<T> createDomainResult(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					String resultVariable,
					DomainResultCreationState creationState) {
				return ( (ModelPart) entityPersister.getIdentifierType() ).createDomainResult(
						navigablePath,
						tableGroup,
						resultVariable,
						creationState
				);
			}

			@Override
			public void applySqlSelections(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					DomainResultCreationState creationState) {
				( (ModelPart) entityPersister.getIdentifierType() ).applySqlSelections(
						navigablePath,
						tableGroup,
						creationState
				);
			}
		};
	}

	public static EntityIdentifierMapping buildNonEncapsulatedCompositeIdentifierMapping(EntityPersister entityPersister) {
		return new EntityIdentifierMapping() {

			@Override
			public MappingType getMappedTypeDescriptor() {
				return null;
			}

			@Override
			public <T> DomainResult<T> createDomainResult(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					String resultVariable,
					DomainResultCreationState creationState) {
				return ( (ModelPart) entityPersister.getIdentifierType() ).createDomainResult(
						navigablePath,
						tableGroup,
						resultVariable,
						creationState
				);
			}

			@Override
			public void applySqlSelections(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					DomainResultCreationState creationState) {
				( (ModelPart) entityPersister.getIdentifierType() ).applySqlSelections(
						navigablePath,
						tableGroup,
						creationState
				);
			}
		};
	}
}
