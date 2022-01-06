/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.metamodel.EmbeddableInstantiator;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.Type;

/**
 * The mapping for a component, composite element,
 * composite identifier, etc.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Component extends SimpleValue implements MetaAttributable, SortableValue {
	private ArrayList<Property> properties = new ArrayList<>();
	private int[] originalPropertyOrder = ArrayHelper.EMPTY_INT_ARRAY;
	private String componentClassName;
	private boolean embedded;
	private String parentProperty;
	private PersistentClass owner;
	private boolean dynamic;
	private Map metaAttributes;
	private boolean isKey;
	private String roleName;

	private Class<? extends EmbeddableInstantiator> customInstantiator;

	// cache the status of the type
	private volatile Type type;

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

	public int getPropertySpan() {
		return properties.size();
	}

	public Iterator<Property> getPropertyIterator() {
		return properties.iterator();
	}

	public void addProperty(Property p) {
		properties.add( p );
	}

	@Override
	public void addColumn(Column column) {
		throw new UnsupportedOperationException("Cant add a column to a component");
	}

	@Override
	public int getColumnSpan() {
		int n=0;
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property p = (Property) iter.next();
			n+= p.getColumnSpan();
		}
		return n;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Selectable> getColumnIterator() {
		Iterator<Selectable>[] iters = new Iterator[ getPropertySpan() ];
		Iterator<Property> iter = getPropertyIterator();
		int i = 0;
		while ( iter.hasNext() ) {
			iters[i++] = iter.next().getColumnIterator();
		}
		return new JoinedIterator<>( iters );
	}

	@Override
	public List<Selectable> getSelectables() {
		final List<Selectable> columns = new ArrayList<>();
		final Iterator<Property> propertyIterator = getPropertyIterator();
		while ( propertyIterator.hasNext() ) {
			final Iterator<Selectable> columnIterator = propertyIterator.next().getColumnIterator();
			while ( columnIterator.hasNext() ) {
				columns.add( columnIterator.next() );
			}
		}
		return columns;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public String getComponentClassName() {
		return componentClassName;
	}

	public Class getComponentClass() throws MappingException {
		final ClassLoaderService classLoaderService = getMetadata()
				.getMetadataBuildingOptions()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );
		try {
			return classLoaderService.classForName( componentClassName );
		}
		catch (ClassLoadingException e) {
			throw new MappingException("component class not found: " + componentClassName, e);
		}
	}

	public PersistentClass getOwner() {
		return owner;
	}

	public String getParentProperty() {
		return parentProperty;
	}

	public void setComponentClassName(String componentClass) {
		this.componentClassName = componentClass;
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

	@Override
	public Type getType() throws MappingException {
		// Resolve the type of the value once and for all as this operation generates a proxy class
		// for each invocation.
		// Unfortunately, there's no better way of doing that as none of the classes are immutable and
		// we can't know for sure the current state of the property or the value.
		Type localType = type;

		if ( localType == null ) {
			synchronized ( this ) {
				if ( type == null ) {
					// Make sure this is sorted which is important especially for synthetic components
					// Other components should be sorted already
					sortProperties( true );

					localType = isEmbedded()
							? new EmbeddedComponentType( this, originalPropertyOrder, getBuildingContext() )
							: new ComponentType( this, originalPropertyOrder, getBuildingContext() );

					this.type = localType;
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
	public Map getMetaAttributes() {
		return metaAttributes;
	}

	@Override
	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes==null?null:(MetaAttribute) metaAttributes.get(attributeName);
	}

	@Override
	public void setMetaAttributes(Map metas) {
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
				&& Objects.equals( parentProperty, other.parentProperty )
				&& Objects.equals( metaAttributes, other.metaAttributes );
	}

	@Override
	public boolean[] getColumnInsertability() {
		boolean[] result = new boolean[ getColumnSpan() ];
		Iterator iter = getPropertyIterator();
		int i=0;
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			boolean[] chunk = prop.getValue().getColumnInsertability();
			if ( prop.isInsertable() ) {
				System.arraycopy(chunk, 0, result, i, chunk.length);
			}
			i+=chunk.length;
		}
		return result;
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		for ( int i = 0; i < properties.size(); i++ ) {
			final Property property = properties.get( i );
			if ( property.getValue().hasAnyInsertableColumns() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		boolean[] result = new boolean[ getColumnSpan() ];
		Iterator iter = getPropertyIterator();
		int i=0;
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			boolean[] chunk = prop.getValue().getColumnUpdateability();
			if ( prop.isUpdateable() ) {
				System.arraycopy(chunk, 0, result, i, chunk.length);
			}
			i+=chunk.length;
		}
		return result;
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		for ( int i = 0; i < properties.size(); i++ ) {
			final Property property = properties.get( i );
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
	 * @throws IndexOutOfBoundsException - if the index is out of range(index < 0 || index >=
	 * {@link #getPropertySpan()})
	 */
	public Property getProperty(int index) {
		return properties.get( index );
	}

	public Property getProperty(String propertyName) throws MappingException {
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( prop.getName().equals(propertyName) ) {
				return prop;
			}
		}
		throw new MappingException("component: " + componentClassName + " property not found: " + propertyName);
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + properties.toString() + ')';
	}

	private IdentifierGenerator builtIdentifierGenerator;

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException {
		if ( builtIdentifierGenerator == null ) {
			builtIdentifierGenerator = buildIdentifierGenerator(
					identifierGeneratorFactory,
					dialect,
					defaultCatalog,
					defaultSchema,
					rootClass
			);
		}
		return builtIdentifierGenerator;
	}

	private IdentifierGenerator buildIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException {
		final boolean hasCustomGenerator = ! DEFAULT_ID_GEN_STRATEGY.equals( getIdentifierGeneratorStrategy() );
		if ( hasCustomGenerator ) {
			return super.createIdentifierGenerator(
					identifierGeneratorFactory, dialect, defaultCatalog, defaultSchema, rootClass
			);
		}

		final Class entityClass = rootClass.getMappedClass();
		final Class attributeDeclarer; // what class is the declarer of the composite pk attributes
		CompositeNestedGeneratedValueGenerator.GenerationContextLocator locator;

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
			// we have the "straight up" embedded (again the hibernate term) component identifier
			attributeDeclarer = entityClass;
		}

		locator = new StandardGenerationContextLocator( rootClass.getEntityName() );
		final CompositeNestedGeneratedValueGenerator generator = new CompositeNestedGeneratedValueGenerator( locator );

		Iterator itr = getPropertyIterator();
		while ( itr.hasNext() ) {
			final Property property = (Property) itr.next();
			if ( property.getValue().isSimpleValue() ) {
				final SimpleValue value = (SimpleValue) property.getValue();

				if ( DEFAULT_ID_GEN_STRATEGY.equals( value.getIdentifierGeneratorStrategy() ) ) {
					// skip any 'assigned' generators, they would have been handled by
					// the StandardGenerationContextLocator
					continue;
				}

				final IdentifierGenerator valueGenerator = value.createIdentifierGenerator(
						identifierGeneratorFactory,
						dialect,
						defaultCatalog,
						defaultSchema,
						rootClass
				);
				generator.addGeneratedValuePlan(
						new ValueGenerationPlan(
								valueGenerator,
								injector( property, attributeDeclarer )
						)
				);
			}
		}
		return generator;
	}

	private Setter injector(Property property, Class attributeDeclarer) {
		return property.getPropertyAccessStrategy( attributeDeclarer )
				.buildPropertyAccess( attributeDeclarer, property.getName(), true )
				.getSetter();
	}

	private Class resolveComponentClass() {
		try {
			return getComponentClass();
		}
		catch ( Exception e ) {
			return null;
		}
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
		private final IdentifierGenerator subGenerator;
		private final Setter injector;

		public ValueGenerationPlan(
				IdentifierGenerator subGenerator,
				Setter injector) {
			this.subGenerator = subGenerator;
			this.injector = injector;
		}

		@Override
		public void execute(SharedSessionContractImplementor session, Object incomingObject, Object injectionContext) {
			final Object generatedValue = subGenerator.generate( session, incomingObject );
			injector.set( injectionContext, generatedValue );
		}

		@Override
		public void registerExportables(Database database) {
			subGenerator.registerExportables( database );
		}

		@Override
		public void initialize(SqlStringGenerationContext context) {
			subGenerator.initialize( context );
		}
	}

	public void prepareForMappingModel() {
		// This call will initialize the type properly
		getType();
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
		final int[] originalPropertyOrder;
		// We need to capture the original property order if this is an alternate unique key or embedded component property
		// to be able to sort the other side of the foreign key accordingly
		// and also if the source is a XML mapping
		// because XML mappings might refer to this through the defined order
		if ( forceRetainOriginalOrder || isAlternateUniqueKey() || isEmbedded() || getBuildingContext() instanceof MappingDocument ) {
			final Object[] originalProperties = properties.toArray();
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
				for ( int i = 0; i < properties.size(); i++ ) {
					final Iterator<Selectable> columnIterator = properties.get( i ).getColumnIterator();
					while ( columnIterator.hasNext() ) {
						final Selectable selectable = columnIterator.next();
						if ( selectable instanceof Column ) {
							columns.add( (Column) selectable );
						}
					}
				}
			}
		}
		return this.originalPropertyOrder = originalPropertyOrder;
	}

	public Class<? extends EmbeddableInstantiator> getCustomInstantiator() {
		return customInstantiator;
	}

	public void setCustomInstantiator(Class<? extends EmbeddableInstantiator> customInstantiator) {
		this.customInstantiator = customInstantiator;
	}
}
