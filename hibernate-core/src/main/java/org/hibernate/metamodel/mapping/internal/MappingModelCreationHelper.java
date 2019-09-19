/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadata;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityIdentifier

	public static EntityIdentifierMapping buildSimpleIdentifierMapping(
			EntityPersister entityPersister,
			String rootTable,
			String pkColumnName,
			BasicType idType,
			MappingModelCreationProcess creationProcess) {
		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		return new EntityIdentifierMapping() {
			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

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
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
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
								tableGroup.resolveTableReference( rootTable ).getIdentificationVariable(),
								( (BasicValuedMapping) entityPersister.getIdentifierType() ).getJdbcMapping(),
								creationProcess.getCreationContext().getSessionFactory()
						)
				);

				final SqlSelection sqlSelection = expressionResolver.resolveSqlSelection(
						expression,
						idType.getExpressableJavaTypeDescriptor(),
						creationProcess.getCreationContext().getSessionFactory().getTypeConfiguration()
				);

				//noinspection unchecked
				return new BasicResult(
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

	public static EntityIdentifierMapping buildEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			CompositeType cidType,
			MappingModelCreationProcess creationProcess) {
		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		return new EntityIdentifierMapping() {
			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MappingType getMappedTypeDescriptor() {
				return ( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getMappedTypeDescriptor();
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
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

	public static EntityIdentifierMapping buildNonEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			CompositeType cidType,
			MappingModelCreationProcess creationProcess) {
		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		return new EntityIdentifierMapping() {

			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MappingType getMappedTypeDescriptor() {
				return entityPersister;
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-identifier attributes

	public static BasicValuedSingularAttributeMapping buildBasicAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			BasicType attrType,
			String tableExpression,
			String attrColumnName,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final BasicValue.Resolution<?> resolution = ( (BasicValue) bootProperty.getValue() ).resolve();
		final BasicValueConverter valueConverter = resolution.getValueConverter();

		final StateArrayContributorMetadataAccess attributeMetadataAccess = entityMappingType -> new StateArrayContributorMetadata() {
			private final MutabilityPlan mutabilityPlan = resolution.getMutabilityPlan();
			private final boolean nullable = bootProperty.getValue().isNullable();
			private final boolean insertable = bootProperty.isInsertable();
			private final boolean updateable = bootProperty.isUpdateable();
			private final boolean includeInOptimisticLocking = bootProperty.isOptimisticLocked();

			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MutabilityPlan getMutabilityPlan() {
				return mutabilityPlan;
			}

			@Override
			public boolean isNullable() {
				return nullable;
			}

			@Override
			public boolean isInsertable() {
				return insertable;
			}

			@Override
			public boolean isUpdatable() {
				return updateable;
			}

			@Override
			public boolean isIncludedInDirtyChecking() {
				// todo (6.0) : do not believe this is correct
				return updateable;
			}

			@Override
			public boolean isIncludedInOptimisticLocking() {
				return includeInOptimisticLocking;
			}

			@Override
			public CascadeStyle getCascadeStyle() {
				return cascadeStyle;
			}
		};

		final FetchStrategy fetchStrategy = bootProperty.isLazy()
				? new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT )
				: FetchStrategy.IMMEDIATE_JOIN;

		if ( valueConverter != null ) {
			// we want to "decompose" the "type" into its various pieces as expected by the mapping
			assert valueConverter.getRelationalJavaDescriptor() == resolution.getRelationalJavaDescriptor();

			final BasicType<?> mappingBasicType = creationProcess.getCreationContext()
					.getDomainModel()
					.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( valueConverter.getRelationalJavaDescriptor(), resolution.getRelationalSqlTypeDescriptor() );


			return new BasicValuedSingularAttributeMapping(
					attrName,
					stateArrayPosition,
					attributeMetadataAccess,
					fetchStrategy,
					tableExpression,
					attrColumnName,
					valueConverter,
					mappingBasicType,
					mappingBasicType.getJdbcMapping(),
					declaringType,
					propertyAccess
			);
		}
		else {
			return new BasicValuedSingularAttributeMapping(
					attrName,
					stateArrayPosition,
					attributeMetadataAccess,
					fetchStrategy,
					tableExpression,
					attrColumnName,
					null,
					attrType,
					attrType,
					declaringType,
					propertyAccess
			);
		}
	}



	public static EmbeddedAttributeMapping buildEmbeddedAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			CompositeType attrType,
			String tableExpression,
			String[] attrColumnNames,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final StateArrayContributorMetadataAccess attributeMetadataAccess = entityMappingType -> new StateArrayContributorMetadata() {
			private final boolean nullable = bootProperty.getValue().isNullable();
			private final boolean insertable = bootProperty.isInsertable();
			private final boolean updateable = bootProperty.isUpdateable();
			private final boolean includeInOptimisticLocking = bootProperty.isOptimisticLocked();

			private final MutabilityPlan mutabilityPlan;

			{
				if ( updateable ) {
					mutabilityPlan = new MutabilityPlan() {
						@Override
						public boolean isMutable() {
							return true;
						}

						@Override
						public Object deepCopy(Object value) {
							if ( value == null ) {
								return null;
							}

							return attrType.deepCopy( value, creationProcess.getCreationContext().getSessionFactory() );
						}

						@Override
						public Serializable disassemble(Object value) {
							throw new NotYetImplementedFor6Exception( getClass() );
						}

						@Override
						public Object assemble(Serializable cached) {
							throw new NotYetImplementedFor6Exception( getClass() );
						}
					};
				}
				else {
					mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;
				}
			}

			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MutabilityPlan getMutabilityPlan() {
				return mutabilityPlan;
			}

			@Override
			public boolean isNullable() {
				return nullable;
			}

			@Override
			public boolean isInsertable() {
				return insertable;
			}

			@Override
			public boolean isUpdatable() {
				return updateable;
			}

			@Override
			public boolean isIncludedInDirtyChecking() {
				// todo (6.0) : do not believe this is correct
				return updateable;
			}

			@Override
			public boolean isIncludedInOptimisticLocking() {
				return includeInOptimisticLocking;
			}

			@Override
			public CascadeStyle getCascadeStyle() {
				return cascadeStyle;
			}
		};

		final EmbeddableMappingType embeddableMappingType = EmbeddableMappingType.from(
				(Component) bootProperty.getValue(),
				attrType,
				attributeMappingType -> new EmbeddedAttributeMapping(
						attrName,
						stateArrayPosition,
						tableExpression,
						attrColumnNames,
						attributeMetadataAccess,
						FetchStrategy.IMMEDIATE_JOIN,
						attributeMappingType,
						declaringType,
						propertyAccess
				),
				creationProcess
		);

		return (EmbeddedAttributeMapping) embeddableMappingType.getEmbeddedValueMapping();
	}
}
