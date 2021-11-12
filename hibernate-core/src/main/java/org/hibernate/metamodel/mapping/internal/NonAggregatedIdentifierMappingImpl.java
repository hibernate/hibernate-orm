/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * A "non-aggregated" composite identifier.
 * <p>
 * This is an identifier mapped using JPA's {@link jakarta.persistence.MapsId} feature.
 *
 * @author Steve Ebersole
 * @apiNote Technically a MapsId id does not have to be composite; we still handle that this class however
 */
public class NonAggregatedIdentifierMappingImpl extends AbstractCompositeIdentifierMapping {

	private final EmbeddableMappingType mappedIdEmbeddableType;

	public NonAggregatedIdentifierMappingImpl(
			EmbeddableMappingType embeddableDescriptor,
			EntityMappingType entityMapping,
			EmbeddableMappingType mappedIdEmbeddableType,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			String rootTableName,
			MappingModelCreationProcess creationProcess) {
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				rootTableName,
				creationProcess.getCreationContext().getSessionFactory()
		);

		this.mappedIdEmbeddableType = mappedIdEmbeddableType;
	}

	@Override
	public boolean hasContainingClass() {
		return mappedIdEmbeddableType != getEmbeddableTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return mappedIdEmbeddableType;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( hasContainingClass() ) {
			final Object[] result = new Object[mappedIdEmbeddableType.getAttributeMappings().size()];
			for ( int i = 0; i < mappedIdEmbeddableType.getAttributeMappings().size(); i++ ) {
				final AttributeMapping attributeMapping = mappedIdEmbeddableType.getAttributeMappings().get( i );
				Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
				result[i] = attributeMapping.disassemble( o, session );
			}

			return result;
		}

		return getEmbeddableTypeDescriptor().disassemble( value, session );
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		if ( hasContainingClass() ) {
			int span = 0;
			for ( int i = 0; i < mappedIdEmbeddableType.getAttributeMappings().size(); i++ ) {
				final AttributeMapping attributeMapping = mappedIdEmbeddableType.getAttributeMappings().get( i );
				final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
				if ( attributeMapping instanceof ToOneAttributeMapping ) {
					final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
					final ForeignKeyDescriptor fkDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
					final Object identifier = fkDescriptor.getAssociationKeyFromSide(
							o,
							toOneAttributeMapping.getSideNature().inverse(),
							session
					);
					span += fkDescriptor.forEachJdbcValue(
							identifier,
							clause,
							span + offset,
							valuesConsumer,
							session
					);
				}
				else {
					span += attributeMapping.forEachJdbcValue( o, clause, span + offset, valuesConsumer, session );
				}
			}
			return span;
		}
		return super.forEachJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		if ( hasContainingClass() ) {
			final SelectableMappings selectableMappings = getEmbeddableTypeDescriptor();
			final List<ColumnReference> columnReferences = CollectionHelper.arrayList( selectableMappings.getJdbcTypeCount() );
			final NavigablePath navigablePath = tableGroup.getNavigablePath()
					.append( getNavigableRole().getNavigableName() );
			final TableReference defaultTableReference = tableGroup.resolveTableReference(
					navigablePath,
					getContainingTableExpression()
			);
			int offset = 0;
			for ( AttributeMapping attributeMapping : mappedIdEmbeddableType.getAttributeMappings() ) {
				offset += attributeMapping.forEachSelectable(
						offset,
						(columnIndex, selection) -> {
							final TableReference tableReference = selection.getContainingTableExpression().equals(
									defaultTableReference.getTableExpression() )
									? defaultTableReference
									: tableGroup.resolveTableReference(
									navigablePath,
									selection.getContainingTableExpression()
							);
							final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
									.resolveSqlExpression(
											SqlExpressionResolver.createColumnReferenceKey(
													tableReference,
													selection.getSelectionExpression()
											),
											sqlAstProcessingState -> new ColumnReference(
													tableReference.getIdentificationVariable(),
													selection,
													sqlAstCreationState.getCreationContext().getSessionFactory()
											)
									);

							columnReferences.add( (ColumnReference) columnReference );
						}
				);
			}

			return new SqlTuple( columnReferences, this );
		}
		return super.toSqlExpression( tableGroup, clause, walker, sqlAstCreationState );
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		if ( hasContainingClass() ) {
			final Object id = mappedIdEmbeddableType.getRepresentationStrategy().getInstantiator().instantiate(
					null,
					sessionFactory
			);
			final List<AttributeMapping> attributeMappings = getEmbeddableTypeDescriptor().getAttributeMappings();
			final List<AttributeMapping> idClassAttributeMappings = mappedIdEmbeddableType.getAttributeMappings();
			final Object[] propertyValues = new Object[attributeMappings.size()];
			for ( int i = 0; i < propertyValues.length; i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				final Object o = attributeMapping.getPropertyAccess().getGetter().get( entity );
				if ( o == null ) {
					final AttributeMapping idClassAttributeMapping = idClassAttributeMappings.get( i );
					if ( idClassAttributeMapping.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive() ) {
						propertyValues[i] = idClassAttributeMapping.getExpressableJavaTypeDescriptor().getDefaultValue();
					}
					else {
						propertyValues[i] = null;
					}
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				else if ( attributeMapping instanceof ToOneAttributeMapping
						&& !( idClassAttributeMappings.get( i ) instanceof ToOneAttributeMapping ) ) {
					final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
					final ModelPart targetPart = toOneAttributeMapping.getForeignKeyDescriptor().getPart(
							toOneAttributeMapping.getSideNature().inverse()
					);
					if ( targetPart instanceof EntityIdentifierMapping ) {
						propertyValues[i] = ( (EntityIdentifierMapping) targetPart ).getIdentifier( o, session );
					}
					else {
						propertyValues[i] = o;
						assert false;
					}
				}
				else {
					propertyValues[i] = o;
				}
			}
			mappedIdEmbeddableType.setPropertyValues( id, propertyValues );
			return id;
		}
		else {
			return entity;
		}
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		final List<AttributeMapping> mappedIdAttributeMappings = mappedIdEmbeddableType.getAttributeMappings();
		final Object[] propertyValues = new Object[mappedIdAttributeMappings.size()];
		final SessionFactoryImplementor factory = session.getFactory();
		getEmbeddableTypeDescriptor().forEachAttributeMapping(
				(position, attribute) -> {
					final AttributeMapping mappedIdAttributeMapping = mappedIdAttributeMappings.get( position );
					final Object o = mappedIdAttributeMapping.getPropertyAccess().getGetter().get( id );
					if ( attribute instanceof ToOneAttributeMapping && !( mappedIdAttributeMapping instanceof ToOneAttributeMapping ) ) {
						final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attribute;
						final EntityPersister entityPersister = toOneAttributeMapping.getEntityMappingType()
								.getEntityPersister();
						final EntityKey entityKey = session.generateEntityKey( o, entityPersister );
						final PersistenceContext persistenceContext = session.getPersistenceContext();
						// it is conceivable there is a proxy, so check that first
						propertyValues[position] = persistenceContext.getProxy( entityKey );
						if ( propertyValues[position] == null ) {
							// otherwise look for an initialized version
							propertyValues[position] = persistenceContext.getEntity( entityKey );
							if ( propertyValues[position] == null ) {
								// get the association out of the entity itself
								propertyValues[position] = factory.getMetamodel()
										.findEntityDescriptor( entity.getClass() )
										.getPropertyValue( entity, toOneAttributeMapping.getAttributeName() );
							}
						}
					}
					else {
						propertyValues[position] = o;
					}
				}
		);
		getEmbeddableTypeDescriptor().setPropertyValues( entity, propertyValues );
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		assert domainValue instanceof Object[];

		final Object[] values = (Object[]) domainValue;
		assert values.length == mappedIdEmbeddableType.getAttributeMappings().size();

		for ( int i = 0; i < mappedIdEmbeddableType.getAttributeMappings().size(); i++ ) {
			final AttributeMapping attribute = mappedIdEmbeddableType.getAttributeMappings().get( i );
			attribute.breakDownJdbcValues( values[ i ], valueConsumer, session );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		for ( int i = 0; i < mappedIdEmbeddableType.getAttributeMappings().size(); i++ ) {
			mappedIdEmbeddableType.getAttributeMappings().get( i ).applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( int i = 0; i < mappedIdEmbeddableType.getAttributeMappings().size(); i++ ) {
			mappedIdEmbeddableType.getAttributeMappings().get( i ).applySqlSelections(
					navigablePath,
					tableGroup,
					creationState,
					selectionConsumer
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Override
	public String getFetchableName() {
		return EntityIdentifierMapping.ROLE_LOCAL_NAME;
	}

	@Override
	public int getNumberOfFetchables() {
		return mappedIdEmbeddableType.getNumberOfFetchables();
	}
}
