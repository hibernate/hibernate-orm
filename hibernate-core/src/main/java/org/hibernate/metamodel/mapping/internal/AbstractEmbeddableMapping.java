/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.internal.MutableAttributeMappingList;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Base support for EmbeddableMappingType implementations
 */
public abstract class AbstractEmbeddableMapping implements EmbeddableMappingType {
	final protected MutableAttributeMappingList attributeMappings;
	protected SelectableMappings selectableMappings;

	public AbstractEmbeddableMapping(MutableAttributeMappingList attributeMappings) {
		this.attributeMappings = attributeMappings;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return getRepresentationStrategy().getMappedJavaType();
	}

	@Override
	public Object[] getValues(Object compositeInstance) {
		if ( compositeInstance == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
			return new Object[getNumberOfAttributeMappings()];
		}

		final ReflectionOptimizer optimizer = getRepresentationStrategy().getReflectionOptimizer();
		if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return optimizer.getAccessOptimizer().getPropertyValues( compositeInstance );
		}

		final Object[] results = new Object[getNumberOfAttributeMappings()];
		for ( int i = 0; i < results.length; i++ ) {
			final Getter getter = getAttributeMapping( i ).getAttributeMetadata()
					.getPropertyAccess()
					.getGetter();
			results[i] = getter.get( compositeInstance );
		}
		return results;
	}

	@Override
	public void setValues(Object component, Object[] values) {
		final ReflectionOptimizer optimizer = getRepresentationStrategy().getReflectionOptimizer();
		if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
			optimizer.getAccessOptimizer().setPropertyValues( component, values );
		}
		else {
			for ( int i = 0; i < values.length; i++ ) {
				getAttributeMapping( i ).getPropertyAccess().getSetter().set( component, values[i] );
			}
		}
	}

	@FunctionalInterface
	protected interface ConcreteTableResolver {
		String resolve(Column column, JdbcEnvironment jdbcEnvironment);
	}

	@FunctionalInterface
	protected interface SuccessfulCompletionCallback {
		void success();
	}

	protected static class IllegalAttributeType extends RuntimeException {
		public IllegalAttributeType(String message) {
			super( message );
		}
	}

	@FunctionalInterface
	protected interface AttributeTypeValidator {
		void check(String name, Type type) throws IllegalAttributeType;
	}

	protected boolean inverseInitializeCallback(
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			EmbeddableMappingType inverseMappingType,
			MappingModelCreationProcess creationProcess,
			ManagedMappingType declaringType,
			MutableAttributeMappingList mappings) {
		final int size = inverseMappingType.getNumberOfAttributeMappings();
		if ( size == 0 ) {
			return false;
		}
		// Reset the attribute mappings that were added in previous attempts
		mappings.clear();
		int currentIndex = 0;
		// We copy the attributes from the inverse mappings and replace the selection mappings
		for ( int j = 0; j < size; j++ ) {
			AttributeMapping attributeMapping = inverseMappingType.getAttributeMapping( j );
			if ( attributeMapping instanceof BasicAttributeMapping ) {
				final BasicAttributeMapping original = (BasicAttributeMapping) attributeMapping;
				final SelectableMapping selectableMapping = selectableMappings.getSelectable( currentIndex );
				attributeMapping = BasicAttributeMapping.withSelectableMapping(
						declaringType,
						original,
						original.getPropertyAccess(),
						selectableMapping.isInsertable(),
						selectableMapping.isUpdateable(),
						selectableMapping
				);
				currentIndex++;
			}
			else if ( attributeMapping instanceof ToOneAttributeMapping ) {
				final ToOneAttributeMapping original = (ToOneAttributeMapping) attributeMapping;
				ForeignKeyDescriptor foreignKeyDescriptor = original.getForeignKeyDescriptor();
				if ( foreignKeyDescriptor == null ) {
					// This is expected to happen when processing a
					// PostInitCallbackEntry because the callbacks
					// are not ordered. The exception is caught in
					// MappingModelCreationProcess.executePostInitCallbacks()
					// and the callback is re-queued.
					throw new IllegalStateException( "Not yet ready: " + original );
				}
				final ToOneAttributeMapping toOne = original.copy(
						declaringType,
						declaringTableGroupProducer
				);
				final int offset = currentIndex;
				toOne.setIdentifyingColumnsTableExpression(
						selectableMappings.getSelectable( offset ).getContainingTableExpression()
				);
				toOne.setForeignKeyDescriptor(
						foreignKeyDescriptor.withKeySelectionMapping(
								declaringType,
								declaringTableGroupProducer,
								index -> selectableMappings.getSelectable( offset + index ),
								creationProcess
						)
				);

				attributeMapping = toOne;
				currentIndex += attributeMapping.getJdbcTypeCount();
			}
			else if ( attributeMapping instanceof EmbeddableValuedModelPart ) {
				final SelectableMapping[] subMappings = new SelectableMapping[attributeMapping.getJdbcTypeCount()];
				for ( int i = 0; i < subMappings.length; i++ ) {
					subMappings[i] = selectableMappings.getSelectable( currentIndex++ );
				}
				attributeMapping = MappingModelCreationHelper.createInverseModelPart(
						(EmbeddableValuedModelPart) attributeMapping,
						declaringType,
						declaringTableGroupProducer,
						new SelectableMappingsImpl( subMappings ),
						creationProcess
				);
			}
			else {
				throw new UnsupportedMappingException(
						"Only basic and to-one attributes are supported in composite fks" );
			}
			mappings.add( attributeMapping );
		}
		return true;
	}

	protected boolean finishInitialization(
			NavigableRole navigableRole,
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			EmbeddableMappingType declarer,
			EmbeddableRepresentationStrategy representationStrategy,
			AttributeTypeValidator attributeTypeValidator,
			ConcreteTableResolver concreteTableResolver,
			Consumer<AttributeMapping> attributeConsumer,
			SuccessfulCompletionCallback completionCallback,
			MappingModelCreationProcess creationProcess) {
		final TypeConfiguration typeConfiguration = creationProcess.getCreationContext().getTypeConfiguration();
		final JdbcServices jdbcServices = creationProcess.getCreationContext().getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final Type[] subtypes = compositeType.getSubtypes();

		int attributeIndex = 0;
		int columnPosition = 0;

		for ( Property bootPropertyDescriptor : bootDescriptor.getProperties() ) {
			final Type subtype = subtypes[ attributeIndex ];

			attributeTypeValidator.check( bootPropertyDescriptor.getName(), subtype );

			final PropertyAccess propertyAccess = representationStrategy.resolvePropertyAccess( bootPropertyDescriptor );
			final AttributeMapping attributeMapping;

			final Value value = bootPropertyDescriptor.getValue();
			if ( subtype instanceof BasicType ) {
				final BasicValue basicValue = (BasicValue) value;
				final Selectable selectable = basicValue.getColumn();
				final String containingTableExpression;
				final String columnExpression;
				if ( rootTableKeyColumnNames == null ) {
					if ( selectable.isFormula() ) {
						columnExpression = selectable.getTemplate(
								dialect,
								creationProcess.getCreationContext().getTypeConfiguration(),
								creationProcess.getSqmFunctionRegistry()
						);
					}
					else {
						columnExpression = selectable.getText( dialect );
					}

					if ( selectable instanceof Column ) {
						containingTableExpression = concreteTableResolver.resolve( (Column) selectable, jdbcEnvironment );
					}
					else {
						containingTableExpression = rootTableExpression;
					}
				}
				else {
					containingTableExpression = rootTableExpression;
					columnExpression = rootTableKeyColumnNames[ columnPosition ];
				}
				final SelectablePath selectablePath;
				final String columnDefinition;
				final Long length;
				final Integer precision;
				final Integer scale;
				final boolean nullable;
				if ( selectable instanceof Column ) {
					final Column column = (Column) selectable;
					columnDefinition = column.getSqlType();
					length = column.getLength();
					precision = column.getPrecision();
					scale = column.getScale();
					nullable = column.isNullable();
					selectablePath = basicValue.createSelectablePath( column.getQuotedName( dialect ) );
				}
				else {
					columnDefinition = null;
					length = null;
					precision = null;
					scale = null;
					nullable = true;
					selectablePath = basicValue.createSelectablePath( bootPropertyDescriptor.getName() );
				}

				attributeMapping = MappingModelCreationHelper.buildBasicAttributeMapping(
						bootPropertyDescriptor.getName(),
						navigableRole.append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						declarer,
						(BasicType<?>) subtype,
						containingTableExpression,
						columnExpression,
						selectablePath,
						selectable.isFormula(),
						selectable.getCustomReadExpression(),
						selectable.getWriteExpr( ( (BasicType<?>) subtype ).getJdbcMapping(), dialect ),
						columnDefinition,
						length,
						precision,
						scale,
						nullable,
						value.isColumnInsertable( 0 ),
						value.isColumnUpdateable( 0 ),
						propertyAccess,
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition++;
			}
			else if ( subtype instanceof AnyType ) {
				final Any bootValueMapping = (Any) value;
				final AnyType anyType = (AnyType) subtype;

				final boolean nullable = bootValueMapping.isNullable();
				final boolean insertable = value.isColumnInsertable( 0 );
				final boolean updateable = value.isColumnUpdateable( 0 );
				final boolean includeInOptimisticLocking = bootPropertyDescriptor.isOptimisticLocked();
				final CascadeStyle cascadeStyle = compositeType.getCascadeStyle( attributeIndex );

				SimpleAttributeMetadata attributeMetadataAccess = new SimpleAttributeMetadata(
						propertyAccess,
						getMutabilityPlan( updateable ),
						nullable,
						insertable,
						updateable,
						includeInOptimisticLocking,
						true,
						cascadeStyle
				);

				attributeMapping = new DiscriminatedAssociationAttributeMapping(
						navigableRole.append( bootPropertyDescriptor.getName() ),
						typeConfiguration.getJavaTypeRegistry().getDescriptor( Object.class ),
						declarer,
						attributeIndex,
						attributeIndex,
						attributeMetadataAccess,
						bootPropertyDescriptor.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE,
						propertyAccess,
						bootPropertyDescriptor,
						anyType,
						bootValueMapping,
						creationProcess
				);
			}
			else if ( subtype instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtype;
				final int columnSpan = subCompositeType.getColumnSpan( creationProcess.getCreationContext().getMetadata() );
				final String subTableExpression;
				final String[] subRootTableKeyColumnNames;
				if ( rootTableKeyColumnNames == null ) {
					subTableExpression = rootTableExpression;
					subRootTableKeyColumnNames = null;
				}
				else {
					subTableExpression = rootTableExpression;
					subRootTableKeyColumnNames = new String[ columnSpan ];
					System.arraycopy( rootTableKeyColumnNames, columnPosition, subRootTableKeyColumnNames, 0, columnSpan );
				}

				attributeMapping = MappingModelCreationHelper.buildEmbeddedAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						declarer,
						subCompositeType,
						subTableExpression,
						subRootTableKeyColumnNames,
						propertyAccess,
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition += columnSpan;
			}
			else if ( subtype instanceof CollectionType ) {
				final EntityPersister entityPersister = creationProcess.getEntityPersister( bootDescriptor.getOwner()
						.getEntityName() );

				attributeMapping = MappingModelCreationHelper.buildPluralAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						entityPersister,
						propertyAccess,
						compositeType.getCascadeStyle( attributeIndex ),
						compositeType.getFetchMode( attributeIndex ),
						creationProcess
				);
			}
			else if ( subtype instanceof EntityType ) {
				final EntityPersister entityPersister = creationProcess.getEntityPersister( bootDescriptor.getOwner()
						.getEntityName() );

				attributeMapping = MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
						bootPropertyDescriptor.getName(),
						navigableRole.append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						entityPersister,
						entityPersister,
						(EntityType) subtype,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);
				columnPosition += bootPropertyDescriptor.getColumnSpan();
			}
			else {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to determine attribute nature : %s#%s",
								bootDescriptor.getOwner().getEntityName(),
								bootPropertyDescriptor.getName()
						)
				);
			}

			attributeConsumer.accept( attributeMapping );

			attributeIndex++;
		}

		completionCallback.success();
		return true;
	}

	@Override
	public int getNumberOfFetchables() {
		return getAttributeMappings().size();
	}

	@Override
	public Fetchable getFetchable(int position) {
		return getAttributeMappings().get( position );
	}

	@Override
	public void visitFetchables(Consumer<? super Fetchable> consumer, EntityMappingType treatTargetType) {
		forEachAttributeMapping( consumer );
	}

	@Override
	public void visitFetchables(IndexedConsumer<? super Fetchable> indexedConsumer, EntityMappingType treatTargetType) {
		this.getAttributeMappings().indexedForEach( indexedConsumer );
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return getAttributeMappings().size();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return getAttributeMappings().get( position );
	}

	@Override
	public AttributeMapping findAttributeMapping(String name) {
		final AttributeMappingsList attributes = getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final AttributeMapping attr = attributes.get( i );
			if ( name.equals( attr.getAttributeName() ) ) {
				return attr;
			}
		}
		return null;
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		checkIsReady();
		return attributeMappings;
	}

	private void checkIsReady() {
		if ( selectableMappings == null ) {
			// This is expected to happen when processing a
			// PostInitCallbackEntry because the callbacks
			// are not ordered. The exception is caught in
			// MappingModelCreationProcess.executePostInitCallbacks()
			// and the callback is re-queued.
			throw new IllegalStateException( "Not yet ready" );
		}
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return getSelectableMappings().getSelectable( columnIndex );
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return getSelectableMappings().forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getSelectableMappings().forEachSelectable( offset, consumer );
	}

	@Override
	public int getJdbcTypeCount() {
		return getSelectableMappings().getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getSelectableMappings().forEachSelectable(
				offset,
				(index, selectable) -> action.accept( index, selectable.getJdbcMapping() )
		);
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return getSelectableMappings().getJdbcMappings();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return getSelectable( index ).getJdbcMapping();
	}

	@Override
	public void forEachAttributeMapping(final IndexedConsumer<? super AttributeMapping> consumer) {
		getAttributeMappings().indexedForEach( consumer );
	}

	@Override
	public void forEachAttributeMapping(final Consumer<? super AttributeMapping> action) {
		getAttributeMappings().forEach( action );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return findAttributeMapping( name );
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		final AttributeMappingsList attributes = getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			consumer.accept( i, attributes.get(i) );
		}
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		forEachAttributeMapping( consumer );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final MutableAttributeMappingList attributes = attributeMappings;
		final int size = attributes.size();
		final Object[] result = new Object[ size ];
		for ( int i = 0; i < size; i++ ) {
			final AttributeMapping attributeMapping = attributes.get( i );
			final Object o = attributeMapping.getValue( value );
			result[i] = attributeMapping.disassemble( o, session );
		}

		return result;
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		int span = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping mapping = attributeMappings.get( i );
			span += mapping.forEachDisassembledJdbcValue( values[i], span + offset, x, y, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;

		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping instanceof PluralAttributeMapping ) {
				continue;
			}
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			span += attributeMapping.forEachJdbcValue( o, span + offset, x, y, valuesConsumer, session );
		}
		return span;
	}

	protected void addAttribute(AttributeMapping attributeMapping) {
		// check if we've already seen this attribute...
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping previous = attributeMappings.get( i );
			if ( attributeMapping.getAttributeName().equals( previous.getAttributeName() ) ) {
				attributeMappings.setAttributeMapping( i, attributeMapping );
				return;
			}
		}

		attributeMappings.add( attributeMapping );
	}

	protected SelectableMappings getSelectableMappings() {
		checkIsReady();
		return selectableMappings;
	}

	protected boolean initColumnMappings() {
		final int propertySpan = attributeMappings.size();
		final List<SelectableMapping> selectableMappings = CollectionHelper.arrayList( propertySpan );

		attributeMappings.indexedForEach(
				(index, attributeMapping) -> attributeMapping.forEachSelectable(
						(columnIndex, selection) -> selectableMappings.add( selection )
				)
		);

		this.selectableMappings = new SelectableMappingsImpl( selectableMappings.toArray( new SelectableMapping[0] ) );

		return true;
	}

	private static MutabilityPlan<?> getMutabilityPlan(boolean updateable) {
		if ( updateable ) {
			return new MutabilityPlan<>() {
				@Override
				public boolean isMutable() {
					return true;
				}

				@Override
				public Object deepCopy(Object value) {
					return value;
				}

				@Override
				public Serializable disassemble(Object value, SharedSessionContract session) {
					throw new UnsupportedOperationException();
				}

				@Override
				public Object assemble(Serializable cached, SharedSessionContract session) {
					throw new UnsupportedOperationException();
				}
			};
		}
		else {
			return ImmutableMutabilityPlan.INSTANCE;
		}
	}
}
