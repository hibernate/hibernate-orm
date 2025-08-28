/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.jpa.event.internal.EmbeddableCallback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.MappingContext;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.boot.model.internal.BinderHelper.renderCascadeTypeList;
import static org.hibernate.cfg.MappingSettings.WRAPPER_ARRAY_HANDLING;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.BASIC;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.MAP;

/**
 * A mapping model object representing a property or field of an {@linkplain PersistentClass entity}
 * or {@linkplain Component embeddable class}.
 *
 * @author Gavin King
 */
public class Property implements Serializable, MetaAttributable {
	private String name;
	private Value value;
	private String cascade;
	private boolean updatable = true;
	private boolean insertable = true;
	private boolean selectable = true;
	private boolean optimisticLocked = true;
	private GeneratorCreator generatorCreator;
	private String propertyAccessorName;
	private PropertyAccessStrategy propertyAccessStrategy;
	private boolean lazy;
	private String lazyGroup;
	private boolean optional;
	private java.util.Map<String,MetaAttribute> metaAttributes;
	private PersistentClass persistentClass;
	private boolean naturalIdentifier;
	private boolean isGeneric;
	private boolean lob;
	private java.util.List<CallbackDefinition> callbackDefinitions;
	private String returnedClassName;

	public boolean isBackRef() {
		return false;
	}

	/**
	 * Does this property represent a synthetic property?  A synthetic property is one we create during
	 * metamodel binding to represent a collection of columns but which does not represent a property
	 * physically available on the entity.
	 *
	 * @return True if synthetic; false otherwise.
	 */
	public boolean isSynthetic() {
		return false;
	}

	public Type getType() throws MappingException {
		return value.getType();
	}

	public int getColumnSpan() {
		return value.getColumnSpan();
	}

	/**
	 * Delegates to {@link Value#getSelectables()}.
	 */
	public java.util.List<Selectable> getSelectables() {
		return value.getSelectables();
	}

	/**
	 * Delegates to {@link Value#getColumns()}.
	 *
	 * @throws org.hibernate.AssertionFailure if the mapping involves formulas
	 */
	public java.util.List<Column> getColumns() {
		return value.getColumns();
	}

	public String getName() {
		return name;
	}

	public boolean isComposite() {
		return value instanceof Component;
	}

	public Value getValue() {
		return value;
	}

	public void resetUpdateable(boolean updateable) {
		setUpdatable( updateable );
		final boolean[] columnUpdateability = getValue().getColumnUpdateability();
		final int columnSpan = getColumnSpan();
		for ( int i = 0; i < columnSpan; i++ ) {
			columnUpdateability[i] = updateable;
		}
	}

	public void resetOptional(boolean optional) {
		setOptional( optional );
		for ( Selectable selectable: getValue().getSelectables() ) {
			if ( selectable instanceof Column column ) {
				column.setNullable( optional );
			}
		}
	}

	public OnDeleteAction getOnDeleteAction() {
		return value instanceof ToOne toOne ? toOne.getOnDeleteAction() : null;
	}

	public CascadeStyle getCascadeStyle() throws MappingException {
		final Type type = value.getType();
		if ( type instanceof AnyType ) {
			return getCascadeStyle( cascade );
		}
		else if ( type instanceof ComponentType componentType ) {
			return getCompositeCascadeStyle( componentType, cascade );
		}
		else if ( type instanceof CollectionType ) {
			final Collection collection = (Collection) value;
			return getCollectionCascadeStyle( collection.getElement().getType(), cascade );
		}
		else {
			return getCascadeStyle( cascade );
		}
	}
//
//	private static CascadeStyle getCompositeCascadeStyle(CompositeType compositeType, String cascade) {
//		if ( compositeType instanceof AnyType ) {
//			return getCascadeStyle( cascade );
//		}
//		else {
//			return getCompositeCascadeStyle( (ComponentType) compositeType, cascade );
//		}
//	}

	private static CascadeStyle getCompositeCascadeStyle(ComponentType compositeType, String cascade) {
		final int length = compositeType.getSubtypes().length;
		for ( int i=0; i<length; i++ ) {
			if ( compositeType.getCascadeStyle(i) != CascadeStyles.NONE ) {
				return CascadeStyles.ALL;
			}
		}
		return getCascadeStyle( cascade );
	}

	private static CascadeStyle getCollectionCascadeStyle(Type elementType, String cascade) {
		if ( elementType instanceof AnyType ) {
			return getCascadeStyle( cascade );
		}
		else if ( elementType instanceof ComponentType componentType ) {
			return getCompositeCascadeStyle( componentType, cascade );
		}
		else {
			return getCascadeStyle( cascade );
		}
	}

	private static CascadeStyle getCascadeStyle(String cascade) {
		if ( cascade==null || cascade.equals("none") || isBlank(cascade) ) {
			return CascadeStyles.NONE;
		}
		else {
			final var tokens = new StringTokenizer(cascade, ", ");
			final int length = tokens.countTokens();
			if ( length == 1) {
				return CascadeStyles.getCascadeStyle( tokens.nextToken() );
			}
			else {
				final CascadeStyle[] styles = new CascadeStyle[length];
				int i = 0;
				while ( tokens.hasMoreTokens() ) {
					styles[i++] = CascadeStyles.getCascadeStyle( tokens.nextToken() );
				}
				return new CascadeStyles.MultipleCascadeStyle( styles );
			}
		}
	}

	public String getCascade() {
		return cascade;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public void setCascade(EnumSet<CascadeType> cascadeTypes) {
		cascade = cascadeTypes == null ? null : renderCascadeTypeList( cascadeTypes );
	}

	public void setName(String name) {
		this.name = name==null ? null : name.intern();
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean isUpdatable() {
		return updatable && value.hasAnyUpdatableColumns();
	}

	/**
	 * @deprecated Use {@link #isUpdatable()}.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public boolean isUpdateable() {
		// if the property mapping consists of all formulas,
		// make it non-updatable
		return isUpdatable();
	}

	public boolean isInsertable() {
		// if the property mapping consists of all formulas,
		// make it non-insertable
		return insertable && value.hasAnyInsertableColumns();
	}

	@Internal
	public GeneratorCreator getValueGeneratorCreator() {
		return generatorCreator;
	}

	@Internal
	public void setValueGeneratorCreator(GeneratorCreator generator) {
		this.generatorCreator = generator;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	/**
	 * @deprecated Use {@link #setUpdatable(boolean)}.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public void setUpdateable(boolean mutable) {
		this.updatable = mutable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String string) {
		propertyAccessorName = string;
	}

	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return propertyAccessStrategy;
	}

	public void setPropertyAccessStrategy(PropertyAccessStrategy propertyAccessStrategy) {
		this.propertyAccessStrategy = propertyAccessStrategy;
	}

	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName == null
			|| BASIC.getExternalName().equals( propertyAccessorName );
	}

	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes==null ? null : metaAttributes.get(attributeName);
	}

	public void setMetaAttributes(Map<String, MetaAttribute> metas) {
		this.metaAttributes = metas;
	}

	public boolean isValid(MappingContext mappingContext) throws MappingException {
		final Value value = getValue();
		if ( value instanceof BasicValue basicValue && basicValue.isDisallowedWrapperArray() ) {
			throw new MappingException(
					"""
					The property %s.%s uses a wrapper type Byte[]/Character[] which indicates an issue in your domain model. \
					These types have been treated like byte[]/char[] until Hibernate 6.2 which meant that null elements were \
					not allowed, but on JDBC were processed like VARBINARY or VARCHAR. If you don't use nulls in your arrays, \
					change the type of the property to byte[]/char[]. To allow explicit uses of the types Byte[]/Character[], \
					allowing null elements, but with a different serialization format than before Hibernate 6.2, configure \
					the setting '%s' to the value '%s'. To revert to the legacy treatment of these types, configure the value to '%s'. \
					For more information on this matter, consult the migration guide of Hibernate 6.2 and the Javadoc of the \
					field 'org.hibernate.cfg.AvailableSettings.WRAPPER_ARRAY_HANDLING'.\
					"""
							.formatted( persistentClass.getEntityName(), name, WRAPPER_ARRAY_HANDLING,
									WrapperArrayHandling.ALLOW, WrapperArrayHandling.LEGACY )
			);
		}
		return value.isValid( mappingContext );
	}

	public String toString() {
		return getClass().getSimpleName() + '(' + name + ')';
	}

	public void setLazy(boolean lazy) {
		this.lazy=lazy;
	}

	/**
	 * Is this property lazy in the "bytecode" sense?
	 * <p>
	 * Lazy here means whether we initialize this field of the entity
	 * instance in its "base fetch group". It affects whether we list
	 * this property's columns in the SQL select for the owning entity
	 * when we load its "base fetch group". The actual value that is set
	 * varies based on the nature (basic, or whatever) of the property.
	 *
	 * @apiNote This method reports whether the property is considered
	 * part of the base fetch group based solely on the information in
	 * the mapping but {@link EnhancementHelper#includeInBaseFetchGroup}
	 * is also accounts for other details.
	 */
	public boolean isLazy() {
		// For a many-to-one, this is always false. Whether the
		// association is EAGER, PROXY or NO-PROXY we always want
		// to select the foreign key
		return !(value instanceof ToOne) && lazy;
	}

	public String getLazyGroup() {
		return lazyGroup;
	}

	public void setLazyGroup(String lazyGroup) {
		this.lazyGroup = lazyGroup;
	}

	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public boolean isSelectable() {
		return selectable;
	}

	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	private PropertyAccess buildPropertyAccess(Class<?> clazz) {
		return getPropertyAccessStrategy( clazz ).buildPropertyAccess( clazz, name, true );
	}

	// todo : remove
	@Internal
	public Getter getGetter(Class<?> clazz) throws MappingException {
		return buildPropertyAccess( clazz ).getGetter();
	}

	// todo : remove
	@Internal
	public Setter getSetter(Class<?> clazz) throws MappingException {
		return buildPropertyAccess( clazz ).getSetter();
	}

	// todo : remove
	@Internal
	public PropertyAccessStrategy getPropertyAccessStrategy(Class<?> clazz) throws MappingException {
		final var propertyAccessStrategy = getPropertyAccessStrategy();
		if ( propertyAccessStrategy != null ) {
			return propertyAccessStrategy;
		}
		else {
			return getPropertyAccessStrategyResolver()
					.resolvePropertyAccessStrategy(
							clazz,
							propertyAccessorName( clazz ),
							isMapEntity( clazz )
									? RepresentationMode.MAP
									: RepresentationMode.POJO
					);
		}
	}

	private PropertyAccessStrategyResolver getPropertyAccessStrategyResolver() {
		return resolveServiceRegistry()
				.requireService( PropertyAccessStrategyResolver.class );
	}

	private String propertyAccessorName(Class<?> clazz) {
		final String accessorName = getPropertyAccessorName();
		if ( accessorName == null ) {
			return isMapEntity( clazz )
					? MAP.getExternalName()
					: BASIC.getExternalName();
		}
		else {
			return accessorName;
		}
	}

	private static boolean isMapEntity(Class<?> clazz) {
		return clazz == null || Map.class.equals( clazz );
	}

	private ServiceRegistry resolveServiceRegistry() {
		if ( getPersistentClass() != null ) {
			return getPersistentClass().getServiceRegistry();
		}
		else if ( getValue() != null ) {
			return getValue().getServiceRegistry();
		}
		else {
			throw new HibernateException( "Could not resolve ServiceRegistry" );
		}
	}

	public boolean isNaturalIdentifier() {
		return naturalIdentifier;
	}

	public void setNaturalIdentifier(boolean naturalIdentifier) {
		this.naturalIdentifier = naturalIdentifier;
	}

	public boolean isGeneric() {
		return isGeneric;
	}

	public void setGeneric(boolean generic) {
		this.isGeneric = generic;
	}

	public boolean isLob() {
		return lob;
	}

	public void setLob(boolean lob) {
		this.lob = lob;
	}

	/**
	 * @deprecated See discussion in {@link EmbeddableCallback}.
	 */
	@Deprecated(since = "7")
	public void addCallbackDefinitions(java.util.List<CallbackDefinition> callbackDefinitions) {
		if ( callbackDefinitions != null && !callbackDefinitions.isEmpty() ) {
			if ( this.callbackDefinitions == null ) {
				this.callbackDefinitions = new ArrayList<>();
			}
			this.callbackDefinitions.addAll( callbackDefinitions );
		}
	}

	/**
	 * @deprecated See discussion in {@link EmbeddableCallback}.
	 */
	@Deprecated(since = "7")
	public java.util.List<CallbackDefinition> getCallbackDefinitions() {
		return callbackDefinitions == null ? emptyList() : unmodifiableList( callbackDefinitions );
	}

	public String getReturnedClassName() {
		return returnedClassName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}

	public Generator createGenerator(RuntimeModelCreationContext context) {
		return generatorCreator == null ? null :
				generatorCreator.createGenerator( new PropertyGeneratorCreationContext( context ) );
	}

	public Property copy() {
		final Property property =
				this instanceof SyntheticProperty
						? new SyntheticProperty()
						: new Property();
		property.setName( getName() );
		property.setValue( getValue() );
		property.setCascade( getCascade() );
		property.setUpdatable( isUpdatable() );
		property.setInsertable( isInsertable() );
		property.setSelectable( isSelectable() );
		property.setOptimisticLocked( isOptimisticLocked() );
		property.setValueGeneratorCreator( getValueGeneratorCreator() );
		property.setPropertyAccessorName( getPropertyAccessorName() );
		property.setPropertyAccessStrategy( getPropertyAccessStrategy() );
		property.setLazy( isLazy() );
		property.setLazyGroup( getLazyGroup() );
		property.setOptional( isOptional() );
		property.setMetaAttributes( getMetaAttributes() );
		property.setPersistentClass( getPersistentClass() );
		property.setNaturalIdentifier( isNaturalIdentifier() );
		property.setGeneric( isGeneric() );
		property.setLob( isLob() );
		property.addCallbackDefinitions( getCallbackDefinitions() );
		property.setReturnedClassName( getReturnedClassName() );
		return property;
	}

	private class PropertyGeneratorCreationContext implements GeneratorCreationContext {
		private final RuntimeModelCreationContext context;

		public PropertyGeneratorCreationContext(RuntimeModelCreationContext context) {
			this.context = context;
		}

		@Override
		public Database getDatabase() {
			return context.getMetadata().getDatabase();
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return context.getBootstrapContext().getServiceRegistry();
		}

		@Override
		public String getDefaultCatalog() {
			return context.getSessionFactoryOptions().getDefaultCatalog();
		}

		@Override
		public String getDefaultSchema() {
			return context.getSessionFactoryOptions().getDefaultSchema();
		}

		@Override
		public PersistentClass getPersistentClass() {
			return persistentClass;
		}

		@Override
		public RootClass getRootClass() {
			return persistentClass.getRootClass();
		}

		@Override
		public Property getProperty() {
			return Property.this;
		}

		@Override
		public SqlStringGenerationContext getSqlStringGenerationContext() {
			return context.getSqlStringGenerationContext();
		}
	}
}
