/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.Remove;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorConverter;
import org.hibernate.metamodel.mapping.internal.DiscriminatorTypeImpl;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.persister.entity.DiscriminatorHelper;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.generator.Generator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.UserComponentType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.usertype.CompositeUserType;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.mapping.MappingHelper.checkPropertyColumnDuplication;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;

/**
 * A mapping model object that represents an {@linkplain jakarta.persistence.Embeddable embeddable class}.
 *
 * @apiNote The name of this class is historical and unfortunate. An embeddable class holds a "component"
 *          of the state of an entity. It has absolutely nothing to do with modularity in software engineering.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Component extends SimpleValue implements MetaAttributable, SortableValue {

	private String componentClassName;
	private boolean embedded;
	private String parentProperty;
	private PersistentClass owner;
	private boolean dynamic;
	private boolean isKey;
	private Boolean isGeneric;
	private String roleName;
	private MappedSuperclass mappedSuperclass;
	private Value discriminator;
	private transient DiscriminatorType<?> discriminatorType;
	private Map<Object, String> discriminatorValues;
	private Map<String, String> subclassToSuperclass;

	private final ArrayList<Property> properties = new ArrayList<>();
	private Map<Property, String> propertyDeclaringClasses;
	private int[] originalPropertyOrder = ArrayHelper.EMPTY_INT_ARRAY;
	private Map<String,MetaAttribute> metaAttributes;

	private Class<? extends EmbeddableInstantiator> customInstantiator;
	private Constructor<?> instantiator;
	private String[] instantiatorPropertyNames;

	// cache the status of the type
	private volatile CompositeType type;

	private AggregateColumn aggregateColumn;
	private AggregateColumn parentAggregateColumn;
	private QualifiedName structName;
	private String[] structColumnNames;
	private transient Class<?> componentClass;
	private transient Boolean simpleRecord;

	private transient Generator builtIdentifierGenerator;

	public Component(MetadataBuildingContext metadata, PersistentClass owner) throws MappingException {
		this( metadata, owner.getTable(), owner );
	}

	public Component(MetadataBuildingContext metadata, Component component) throws MappingException {
		this( metadata, component.getTable(), component.getOwner() );
	}

	public Component(MetadataBuildingContext metadata, Join join) throws MappingException {
		this( metadata, join.getTable(), join.getPersistentClass() );
	}

	public Component(MetadataBuildingContext metadata, Collection collection) throws MappingException {
		this( metadata, collection.getCollectionTable(), collection.getOwner() );
	}

	public Component(MetadataBuildingContext metadata, Table table, PersistentClass owner) throws MappingException {
		super( metadata, table );
		this.owner = owner;

		metadata.getMetadataCollector().registerComponent( this );
	}

	private Component(Component original) {
		super( original );
		this.properties.addAll( original.properties );
		this.originalPropertyOrder = original.originalPropertyOrder == null ? null : original.originalPropertyOrder.clone();
		this.propertyDeclaringClasses = original.propertyDeclaringClasses;
		this.componentClassName = original.componentClassName;
		this.componentClass = original.componentClass;
		this.embedded = original.embedded;
		this.parentProperty = original.parentProperty;
		this.owner = original.owner;
		this.dynamic = original.dynamic;
		this.isGeneric = original.isGeneric;
		this.metaAttributes = original.metaAttributes == null ? null : new HashMap<>( original.metaAttributes );
		this.isKey = original.isKey;
		this.roleName = original.roleName;
		this.discriminator = original.discriminator;
		this.discriminatorValues = original.discriminatorValues;
		this.subclassToSuperclass = original.subclassToSuperclass;
		this.customInstantiator = original.customInstantiator;
		this.type = original.type;
	}

	@Override
	public Component copy() {
		return new Component( this );
	}

	public int getPropertySpan() {
		return properties.size();
	}

	@Deprecated @Remove
	public Iterator<Property> getPropertyIterator() {
		return properties.iterator();
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void addProperty(Property p, XClass declaringClass) {
		properties.add( p );
		if ( isPolymorphic() && declaringClass != null ) {
			if ( propertyDeclaringClasses == null ) {
				propertyDeclaringClasses = new HashMap<>();
			}
			propertyDeclaringClasses.put( p, declaringClass.getName() );
		}
	}

	public void addProperty(Property p) {
		addProperty( p, null );
	}

	public String getPropertyDeclaringClass(Property p) {
		if ( propertyDeclaringClasses != null ) {
			return propertyDeclaringClasses.get( p );
		}
		return null;
	}

	@Override
	public void addColumn(Column column) {
		throw new UnsupportedOperationException("Cant add a column to a component");
	}

	@Override
	public List<Selectable> getSelectables() {
		final List<Selectable> selectables = new ArrayList<>( properties.size() + 2 );
		for ( Property property : properties ) {
			selectables.addAll( property.getSelectables() );
		}
		if ( discriminator != null ) {
			selectables.addAll( discriminator.getSelectables() );
		}
		return unmodifiableList( selectables );
	}

	@Override
	public List<Column> getColumns() {
		final List<Column> columns = new ArrayList<>( properties.size() + 2 );
		for ( Property property : properties ) {
			columns.addAll( property.getValue().getColumns() );
		}
		if ( discriminator != null ) {
			columns.addAll( discriminator.getColumns() );
		}
		return unmodifiableList( columns );
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public AggregateColumn getAggregateColumn() {
		return aggregateColumn;
	}

	public void setAggregateColumn(AggregateColumn aggregateColumn) {
		this.aggregateColumn = aggregateColumn;
		notifyPropertiesAboutAggregateColumn( aggregateColumn, this );
	}

	@Override
	public int getColumnSpan() {
		return getSelectables().size();
	}

	public List<Column> getAggregatedColumns() {
		final List<Column> aggregatedColumns = new ArrayList<>( getPropertySpan() );
		collectAggregatedColumns( aggregatedColumns, this );
		return aggregatedColumns;
	}

	private void collectAggregatedColumns(List<Column> aggregatedColumns, Component component) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component ) {
				final Component subComponent = (Component) value;
				final AggregateColumn subAggregate = subComponent.getAggregateColumn();
				if ( subAggregate != null ) {
					aggregatedColumns.add( subAggregate );
				}
				else {
					collectAggregatedColumns( aggregatedColumns, subComponent );
				}
			}
			else {
				aggregatedColumns.addAll( value.getColumns() );
			}
		}
		if ( component.isPolymorphic() ) {
			aggregatedColumns.addAll( component.getDiscriminator().getColumns() );
		}
	}

	private void notifyPropertiesAboutAggregateColumn(AggregateColumn aggregateColumn, Component component) {
		for ( Property property : component.getProperties() ) {
			// Let the BasicValue of every sub-column know about the aggregate,
			// which is needed in type resolution
			final Value value = property.getValue();
			if ( value instanceof BasicValue ) {
				assert ( (BasicValue) value ).getResolution() == null;
				( (BasicValue) value ).setAggregateColumn( aggregateColumn );
			}
			else if ( value instanceof Component ) {
				final Component subComponent = (Component) value;
				if ( subComponent.getAggregateColumn() == null ) {
					subComponent.notifyPropertiesAboutAggregateColumn( aggregateColumn, subComponent );
				}
				else {
					( (Component) value ).setParentAggregateColumn( aggregateColumn );
				}
			}
		}
		if ( component.isPolymorphic() ) {
			( (BasicValue) component.getDiscriminator() ).setAggregateColumn( aggregateColumn );
		}
	}

	public AggregateColumn getParentAggregateColumn() {
		return parentAggregateColumn;
	}

	public void setParentAggregateColumn(AggregateColumn parentAggregateColumn) {
		this.parentAggregateColumn = parentAggregateColumn;
	}

	public QualifiedName getStructName() {
		return structName;
	}

	public void setStructName(QualifiedName structName) {
		this.structName = structName;
	}

	@Override
	public void checkColumnDuplication(Set<String> distinctColumns, String owner) {
		if ( aggregateColumn == null ) {
			if ( isPolymorphic() ) {
				// We can allow different subtypes reusing the same columns
				// since only one subtype can exist at one time
				final Map<String, Set<String>> distinctColumnsByClass = new HashMap<>();
				for ( Property prop : properties ) {
					if ( prop.isUpdateable() || prop.isInsertable() ) {
						final String declaringClass = propertyDeclaringClasses.get( prop );
						final Set<String> set = distinctColumnsByClass.computeIfAbsent(
								declaringClass,
								k -> new HashSet<>( distinctColumns )
						);
						prop.getValue().checkColumnDuplication( set, owner );
					}
				}
				for ( Set<String> columns : distinctColumnsByClass.values() ) {
					distinctColumns.addAll( columns );
				}
			}
			else {
				checkPropertyColumnDuplication( distinctColumns, getProperties(), owner );
			}
		}
		else {
			checkPropertyColumnDuplication( new HashSet<>(), getProperties(), "component '" + getRoleName() + "'" );
			aggregateColumn.getValue().checkColumnDuplication( distinctColumns, owner );
		}
	}

	public String getComponentClassName() {
		return componentClassName;
	}

	public Class<?> getComponentClass() throws MappingException {
		Class<?> result = componentClass;
		if ( result == null ) {
			if ( componentClassName == null ) {
				return null;
			}
			else {
				try {
					result = componentClass = getMetadata()
							.getMetadataBuildingOptions()
							.getServiceRegistry()
							.requireService( ClassLoaderService.class ).classForName( componentClassName );
				}
				catch (ClassLoadingException e) {
					throw new MappingException( "component class not found: " + componentClassName, e );
				}
			}
		}
		return result;
	}

	public PersistentClass getOwner() {
		return owner;
	}

	public String getParentProperty() {
		return parentProperty;
	}

	public void setComponentClassName(String componentClass) {
		this.componentClassName = componentClass;
		this.componentClass = null;
		this.simpleRecord = null;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public void setOwner(PersistentClass owner) {
		this.owner = owner;
	}

	public void setParentProperty(String parentProperty) {
		this.parentProperty = parentProperty;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	private CompositeUserType<?> createCompositeUserType(Component component) {
		final BootstrapContext bootstrapContext = getBuildingContext().getBootstrapContext();
		final Class<CompositeUserType<?>> customTypeClass =
				bootstrapContext.getClassLoaderAccess().classForName( component.getTypeName() );
		return !getBuildingContext().getBuildingOptions().isAllowExtensionsInCdi()
				? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( customTypeClass )
				: bootstrapContext.getServiceRegistry().requireService( ManagedBeanRegistry.class )
				.getBean( customTypeClass ).getBeanInstance();
	}

	@Override
	public CompositeType getType() throws MappingException {
		// Resolve the type of the value once and for all as this operation generates a proxy class
		// for each invocation.
		// Unfortunately, there's no better way of doing that as none of the classes are immutable,
		// and we can't know for sure the current state of the property or the value.
		CompositeType localType = type;

		if ( localType == null ) {
			synchronized ( this ) {
				localType = type;
				if ( localType == null ) {
					// Make sure this is sorted which is important especially for synthetic components
					// Other components should be sorted already
					sortProperties( true );

					final String typeName = getTypeName();
					if ( typeName == null ) {
						localType = isEmbedded()
								? new EmbeddedComponentType( this, originalPropertyOrder )
								: new ComponentType( this, originalPropertyOrder );
					}
					else {
						final CompositeUserType<?> compositeUserType = createCompositeUserType( this );
						localType = new UserComponentType<>( this, originalPropertyOrder, compositeUserType );
					}

					type = localType;
				}
			}
		}

		return localType;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	@Override
	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	@Override
	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes==null ? null : metaAttributes.get(attributeName);
	}

	@Override
	public void setMetaAttributes(Map<String, MetaAttribute> metas) {
		this.metaAttributes = metas;
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof Component && isSame( (Component) other );
	}

	public boolean isSame(Component other) {
		return super.isSame( other )
			&& Objects.equals( properties, other.properties )
			&& Objects.equals( componentClassName, other.componentClassName )
			&& embedded == other.embedded
			&& Objects.equals( aggregateColumn, other.aggregateColumn )
			&& Objects.equals( parentAggregateColumn, other.parentAggregateColumn )
			&& Objects.equals( structName, other.structName )
			&& Objects.equals( parentProperty, other.parentProperty )
			&& Objects.equals( metaAttributes, other.metaAttributes );
	}

	@Override
	public boolean[] getColumnInsertability() {
		final boolean[] result = new boolean[getColumnSpan()];
		int i = 0;
		for ( Property prop : getProperties() ) {
			i += copyFlags( prop.getValue().getColumnInsertability(), result, i, prop.isInsertable() );
		}
		if ( isPolymorphic() ) {
			i += copyFlags( getDiscriminator().getColumnInsertability(), result, i, true );
		}
		assert i == getColumnSpan();
		return result;
	}

	private static int copyFlags(boolean[] chunk, boolean[] result, int i, boolean doCopy) {
		if ( doCopy ) {
			System.arraycopy( chunk, 0, result, i, chunk.length );
		}
		return chunk.length;
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		for ( Property property : properties ) {
			if ( property.getValue().hasAnyInsertableColumns() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		boolean[] result = new boolean[getColumnSpan()];
		int i = 0;
		for ( Property prop : getProperties() ) {
			i += copyFlags( prop.getValue().getColumnUpdateability(), result, i, prop.isUpdateable() );
		}
		if ( isPolymorphic() ) {
			i += copyFlags( getDiscriminator().getColumnUpdateability(), result, i, true );
		}
		assert i == getColumnSpan();
		return result;
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		for ( Property property : properties ) {
			if ( property.getValue().hasAnyUpdatableColumns() ) {
				return true;
			}
		}
		return false;
	}

	public boolean isKey() {
		return isKey;
	}

	public void setKey(boolean isKey) {
		this.isKey = isKey;
	}

	public boolean hasPojoRepresentation() {
		return componentClassName!=null;
	}

	/**
	 * Returns the {@link Property} at the specified position in this {@link Component}.
	 *
	 * @param index index of the {@link Property} to return
	 * @return {@link Property}
	 * @throws IndexOutOfBoundsException - if the index is out of range(index &lt; 0 || index &gt;=
	 * {@link #getPropertySpan()})
	 */
	public Property getProperty(int index) {
		return properties.get( index );
	}

	public Property getProperty(String propertyName) throws MappingException {
		for ( Property prop : properties ) {
			if ( prop.getName().equals(propertyName) ) {
				return prop;
			}
		}
		throw new MappingException("component: " + componentClassName + " property not found: " + propertyName);
	}

	public boolean matchesAllProperties(String... propertyNames) {
		return properties.size() == propertyNames.length &&
				new HashSet<>(properties.stream().map(Property::getName)
						.collect(toList()))
						.containsAll(List.of(propertyNames));
	}

	public boolean hasProperty(String propertyName) {
		for ( Property prop : properties ) {
			if ( prop.getName().equals(propertyName) ) {
				return true;
			}
		}
		return false;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public MappedSuperclass getMappedSuperclass() {
		return mappedSuperclass;
	}

	public void setMappedSuperclass(MappedSuperclass mappedSuperclass) {
		this.mappedSuperclass = mappedSuperclass;
	}

	public Value getDiscriminator() {
		return discriminator;
	}

	public void setDiscriminator(Value discriminator) {
		this.discriminator = discriminator;
	}

	public DiscriminatorType<?> getDiscriminatorType() {
		DiscriminatorType<?> type = discriminatorType;
		if ( type == null ) {
			type = discriminatorType = buildDiscriminatorType();
		}
		return type;
	}

	private DiscriminatorType<?> buildDiscriminatorType() {
		return getBuildingContext().getMetadataCollector().resolveEmbeddableDiscriminatorType( getComponentClass(), () -> {
			final JavaTypeRegistry javaTypeRegistry = getTypeConfiguration().getJavaTypeRegistry();
			final JavaType<String> domainJavaType = javaTypeRegistry.resolveDescriptor( Class.class );
			final BasicType<?> discriminatorType = DiscriminatorHelper.getDiscriminatorType( this );
			final DiscriminatorConverter<String, ?> converter = EmbeddableDiscriminatorConverter.fromValueMappings(
					qualify( getComponentClassName(), DISCRIMINATOR_ROLE_NAME ),
					domainJavaType,
					discriminatorType,
					getDiscriminatorValues(),
					getServiceRegistry()
			);
			return new DiscriminatorTypeImpl<>( discriminatorType, converter );
		} );
	}

	public boolean isPolymorphic() {
		return discriminator != null;
	}

	public Map<Object, String> getDiscriminatorValues() {
		return discriminatorValues;
	}

	public void setDiscriminatorValues(Map<Object, String> discriminatorValues) {
		this.discriminatorValues = discriminatorValues;
	}

	public String getSuperclass(String subclass) {
		return subclassToSuperclass.get( subclass );
	}

	public void setSubclassToSuperclass(Map<String, String> subclassToSuperclass) {
		this.subclassToSuperclass = subclassToSuperclass;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + componentClassName + ')';
	}

	@Override
	public Generator createGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass) throws MappingException {
		if ( builtIdentifierGenerator == null ) {
			builtIdentifierGenerator = buildIdentifierGenerator(
					identifierGeneratorFactory,
					dialect,
					rootClass
			);
		}
		return builtIdentifierGenerator;
	}

	private Generator buildIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass) throws MappingException {
		final boolean hasCustomGenerator = ! DEFAULT_ID_GEN_STRATEGY.equals( getIdentifierGeneratorStrategy() );
		if ( hasCustomGenerator ) {
			return super.createGenerator( identifierGeneratorFactory, dialect, rootClass );
		}

		final Class<?> entityClass = rootClass.getMappedClass();
		final Class<?> attributeDeclarer; // what class is the declarer of the composite pk attributes
		// IMPL NOTE : See the javadoc discussion on CompositeNestedGeneratedValueGenerator wrt the
		//		various scenarios for which we need to account here
		if ( rootClass.getIdentifierMapper() != null ) {
			// we have the @IdClass / <composite-id mapped="true"/> case
			attributeDeclarer = resolveComponentClass();
		}
		else if ( rootClass.getIdentifierProperty() != null ) {
			// we have the "@EmbeddedId" / <composite-id name="idName"/> case
			attributeDeclarer = resolveComponentClass();
		}
		else {
			// we have the "straight up" embedded (again the Hibernate term) component identifier
			attributeDeclarer = entityClass;
		}

		final CompositeNestedGeneratedValueGenerator.GenerationContextLocator locator =
				new StandardGenerationContextLocator( rootClass.getEntityName() );
		final CompositeNestedGeneratedValueGenerator generator =
				new CompositeNestedGeneratedValueGenerator( locator, getType() );

		final List<Property> properties = getProperties();
		for ( int i = 0; i < properties.size(); i++ ) {
			final Property property = properties.get( i );
			if ( property.getValue().isSimpleValue() ) {
				final SimpleValue value = (SimpleValue) property.getValue();

				if ( !DEFAULT_ID_GEN_STRATEGY.equals( value.getIdentifierGeneratorStrategy() )
						|| value.getCustomIdGeneratorCreator() != null ) {
					// skip any 'assigned' generators, they would have been handled by
					// the StandardGenerationContextLocator
					generator.addGeneratedValuePlan( new ValueGenerationPlan(
							value.createGenerator( identifierGeneratorFactory, dialect, rootClass ),
							getType().isMutable() ? injector( property, attributeDeclarer ) : null,
							i
					) );
				}
			}
		}
		return generator;
	}

	private Setter injector(Property property, Class<?> attributeDeclarer) {
		return property.getPropertyAccessStrategy( attributeDeclarer )
				.buildPropertyAccess( attributeDeclarer, property.getName(), true )
				.getSetter();
	}

	private Class<?> resolveComponentClass() {
		try {
			return getComponentClass();
		}
		catch ( Exception e ) {
			return null;
		}
	}

	@Internal
	public String[] getPropertyNames() {
		final String[] propertyNames = new String[properties.size()];
		for ( int i = 0; i < properties.size(); i++ ) {
			propertyNames[i] = properties.get( i ).getName();
		}
		return propertyNames;
	}

	public void clearProperties() {
		properties.clear();
	}

	public static class StandardGenerationContextLocator
			implements CompositeNestedGeneratedValueGenerator.GenerationContextLocator {
		private final String entityName;

		public StandardGenerationContextLocator(String entityName) {
			this.entityName = entityName;
		}

		@Override
		public Object locateGenerationContext(SharedSessionContractImplementor session, Object incomingObject) {
			return session.getEntityPersister( entityName, incomingObject ).getIdentifier( incomingObject, session );
		}
	}

	public static class ValueGenerationPlan implements CompositeNestedGeneratedValueGenerator.GenerationPlan {
		private final Generator subgenerator;
		private final Setter injector;
		private final int propertyIndex;

		public ValueGenerationPlan(Generator subgenerator, Setter injector, int propertyIndex) {
			this.subgenerator = subgenerator;
			this.injector = injector;
			this.propertyIndex = propertyIndex;
		}

		@Override
		public Setter getInjector() {
			return injector;
		}

		@Override
		public int getPropertyIndex() {
			return propertyIndex;
		}

		@Override
		public BeforeExecutionGenerator getGenerator() {
			if ( subgenerator instanceof BeforeExecutionGenerator ) {
				return (BeforeExecutionGenerator) subgenerator;
			}
			else {
				throw new IdentifierGenerationException( "Identity generation isn't supported for composite ids" );
			}
		}

		@Override
		public void registerExportables(Database database) {
			if ( subgenerator instanceof ExportableProducer ) {
				( (ExportableProducer) subgenerator).registerExportables( database );
			}
		}

		@Override
		public void initialize(SqlStringGenerationContext context) {
			if ( subgenerator instanceof Configurable) {
				( (Configurable) subgenerator).initialize( context );
			}
		}
	}

	public void prepareForMappingModel() {
		// This call will initialize the type properly
		getType();
	}

	@Override
	public boolean isValid(Mapping mapping) throws MappingException {
		if ( !super.isValid( mapping ) ) {
			return false;
		}
		if ( instantiatorPropertyNames != null ) {
			if ( instantiatorPropertyNames.length < properties.size() ) {
				throw new MappingException( "component type [" + componentClassName + "] specifies " + instantiatorPropertyNames.length + " properties for the instantiator but has " + properties.size() + " properties" );
			}
			final HashSet<String> assignedPropertyNames = CollectionHelper.setOfSize( properties.size() );
			for ( String instantiatorPropertyName : instantiatorPropertyNames ) {
				if ( getProperty( instantiatorPropertyName ) == null ) {
					throw new MappingException( "could not find property [" + instantiatorPropertyName + "] defined in the @Instantiator withing component [" + componentClassName + "]" );
				}
				assignedPropertyNames.add( instantiatorPropertyName );
			}
			if ( assignedPropertyNames.size() != properties.size() ) {
				final ArrayList<String> missingProperties = new ArrayList<>();
				for ( Property property : properties ) {
					if ( !assignedPropertyNames.contains( property.getName() ) ) {
						missingProperties.add( property.getName() );
					}
				}
				throw new MappingException( "component type [" + componentClassName + "] has " + properties.size() + " properties but the instantiator only assigns " + assignedPropertyNames.size() + " properties. missing properties: " + missingProperties );
			}
		}
		return true;
	}

	@Override
	public boolean isSorted() {
		return originalPropertyOrder != ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public int[] sortProperties() {
		return sortProperties( false );
	}

	private int[] sortProperties(boolean forceRetainOriginalOrder) {
		if ( originalPropertyOrder != ArrayHelper.EMPTY_INT_ARRAY ) {
			return originalPropertyOrder;
		}
		// Don't sort the properties for a simple record
		if ( isSimpleRecord() ) {
			return this.originalPropertyOrder = null;
		}
		final int[] originalPropertyOrder;
		// We need to capture the original property order if this is an alternate unique key or embedded component property
		// to be able to sort the other side of the foreign key accordingly
		// and also if the source is a XML mapping
		// because XML mappings might refer to this through the defined order
		if ( forceRetainOriginalOrder || isAlternateUniqueKey() || isEmbedded()
				|| getBuildingContext() instanceof MappingDocument ) {
			final Property[] originalProperties = properties.toArray( new Property[0] );
			properties.sort( Comparator.comparing( Property::getName ) );
			originalPropertyOrder = new int[originalProperties.length];
			for ( int j = 0; j < originalPropertyOrder.length; j++ ) {
				originalPropertyOrder[j] = properties.indexOf( originalProperties[j] );
			}
		}
		else {
			properties.sort( Comparator.comparing( Property::getName ) );
			originalPropertyOrder = null;
		}
		if ( isKey ) {
			final PrimaryKey primaryKey = getOwner().getTable().getPrimaryKey();
			if ( primaryKey != null ) {
				// We have to re-order the primary key accordingly
				final List<Column> columns = primaryKey.getColumns();
				columns.clear();
				for ( Property property : properties ) {
					for ( Selectable selectable : property.getSelectables() ) {
						if ( selectable instanceof Column ) {
							columns.add( (Column) selectable );
						}
					}
				}
			}
		}
		return this.originalPropertyOrder = originalPropertyOrder;
	}

	public void setSimpleRecord(boolean simpleRecord) {
		this.simpleRecord = simpleRecord;
	}

	public boolean isSimpleRecord() {
		Boolean simple = simpleRecord;
		if ( simple == null ) {
			// A simple record is given, when the properties match the order of the record component names
			final Class<?> componentClass = resolveComponentClass();
			if ( customInstantiator != null ) {
				return simpleRecord = false;
			}
			if ( componentClass == null || !ReflectHelper.isRecord( componentClass ) ) {
				return simpleRecord = false;
			}
			final String[] recordComponentNames = ReflectHelper.getRecordComponentNames( componentClass );
			if ( recordComponentNames.length != properties.size() ) {
				return simpleRecord = false;
			}
			for ( int i = 0; i < recordComponentNames.length; i++ ) {
				if ( !recordComponentNames[i].equals( properties.get( i ).getName() ) ) {
					return simpleRecord = false;
				}
			}
			simple = simpleRecord = true;
		}
		return simple;
	}

	public Class<? extends EmbeddableInstantiator> getCustomInstantiator() {
		return customInstantiator;
	}

	public void setCustomInstantiator(Class<? extends EmbeddableInstantiator> customInstantiator) {
		this.customInstantiator = customInstantiator;
	}

	public Constructor<?> getInstantiator() {
		return instantiator;
	}

	public String[] getInstantiatorPropertyNames() {
		return instantiatorPropertyNames;
	}

	public void setInstantiator(Constructor<?> instantiator, String[] instantiatorPropertyNames) {
		this.instantiator = instantiator;
		this.instantiatorPropertyNames = instantiatorPropertyNames;
	}

	public String[] getStructColumnNames() {
		return structColumnNames;
	}

	public void setStructColumnNames(String[] structColumnNames) {
		this.structColumnNames = structColumnNames;
	}

	public boolean isGeneric() {
		if ( isGeneric == null ) {
			isGeneric = getComponentClassName() != null && getComponentClass().getTypeParameters().length != 0;
		}
		return isGeneric;
	}

	public void setGeneric(boolean generic) {
		isGeneric = generic;
	}
}
