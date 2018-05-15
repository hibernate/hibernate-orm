/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EmbeddableJavaTypeMapping;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.internal.EmbeddableJavaTypeMappingImpl;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.type.descriptor.java.internal.EmbeddableJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

/**
 * The mapping for a component, composite element,
 * composite identifier, etc.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Component extends SimpleValue
		implements EmbeddedValueMappingImplementor, PropertyContainer, MetaAttributable {
	TreeMap<String, PersistentAttributeMapping> declaredAttributeMappings;
	private String componentClassName;
	private boolean embedded;
	private String parentProperty;
	private PersistentClass owner;
	private boolean dynamic;
	private Map<String, MetaAttribute> metaAttributes;
	private boolean isKey;
	private String roleName;
	private EmbeddableJavaTypeMapping javaTypeMapping;
	private Integer columnSpan;

	public Component(MetadataBuildingContext metadata, PersistentClass owner) throws MappingException {
		this( metadata, owner.getMappedTable(), owner );
	}

	public Component(MetadataBuildingContext metadata, Component component) throws MappingException {
		this( metadata, component.getMappedTable(), component.getOwner() );
	}

	public Component(MetadataBuildingContext metadata, Join join) throws MappingException {
		this( metadata, join.getMappedTable(), join.getPersistentClass() );
	}

	public Component(MetadataBuildingContext metadata, Collection collection) throws MappingException {
		this( metadata, collection.getMappedTable(), collection.getOwner() );
	}

	public Component(MetadataBuildingContext metadata, MappedTable table, PersistentClass owner) throws MappingException {
		super( metadata, table );
		this.owner = owner;
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		if ( javaTypeMapping == null ) {
			javaTypeMapping = new EmbeddableJavaTypeMappingImpl<>( getMetadataBuildingContext(), getName(), componentClassName, null );
		}
		return javaTypeMapping;
	}

	@Override
	public String getName() {
		if ( roleName != null ) {
			return roleName;
		}
		return componentClassName;
	}

	@Override
	public RepresentationMode getExplicitRepresentationMode() {
		return null;
	}

	@Override
	public void setExplicitRepresentationMode(RepresentationMode mode) {
		throw new UnsupportedOperationException(
				"Support for ManagedType-specific explicit RepresentationMode not yet implemented" );
	}

	/**
	 * @deprecated since 6.0 , use {@link #getDeclaredPersistentAttributes().size()} instead.
	 */
	@Deprecated
	public int getPropertySpan() {
		return getDeclaredPersistentAttributes().size();
	}

	/**
	 * @deprecated since 6.0 , use {@link #getDeclaredPersistentAttributes()} instead.
	 */
	@Deprecated
	public Iterator getPropertyIterator() {
		return getDeclaredPersistentAttributes().iterator();
	}

	/**
	 * @deprecated since 6.0, use {@link #addDeclaredPersistentAttribute(PersistentAttributeMapping)} instead.
	 */
	@Deprecated
	public void addProperty(Property p) {
		addDeclaredPersistentAttribute( p );
	}

	@Override
	public void addColumn(Column column) {
		throw new UnsupportedOperationException( "Cant add a column to a component" );
	}


	@Override
	public int getColumnSpan() {
		if ( columnSpan == null ) {
			int i = 0;
			for ( PersistentAttributeMapping persistentAttributeMapping : getDeclaredPersistentAttributes() ) {
				i += persistentAttributeMapping.getValueMapping().getMappedColumns().size();
			}
			columnSpan = i;
		}

		return columnSpan;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	/**
	 * @deprecated since 6.0, use {@link #getEmbeddableClassName()}.
	 */
	@Deprecated
	public String getComponentClassName() {
		return getEmbeddableClassName();
	}

	@Override
	public String getEmbeddableClassName() {
		return componentClassName;
	}

	public Class getComponentClass() throws MappingException {
		final ClassLoaderService classLoaderService = getMetadataBuildingContext().getBuildingOptions()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );
		try {
			return classLoaderService.classForName( componentClassName );
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "component class not found: " + componentClassName, e );
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
	public <X> EmbeddedTypeDescriptor<X> makeRuntimeDescriptor(
			EmbeddedContainer embeddedContainer,
			String localName,
			SingularPersistentAttribute.Disposition disposition,
			RuntimeModelCreationContext context) {
		return context.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
				this,
				embeddedContainer,
				null,
				localName,
				disposition,
				context
		);
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
	}

	@Override
	public java.util.Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	@Override
	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes == null ? null : metaAttributes.get( attributeName );
	}

	@Override
	public void setMetaAttributes(java.util.Map<String, MetaAttribute> metas) {
		this.metaAttributes = metas;
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof Component && isSame( (Component) other );
	}

	public boolean isSame(Component other) {
		return super.isSame( other )
				&& Objects.equals( componentClassName, other.componentClassName )
				&& embedded == other.embedded
				&& Objects.equals( parentProperty, other.parentProperty )
				&& Objects.equals( metaAttributes, other.metaAttributes );
	}

	@Override
	public boolean[] getColumnInsertability() {
		boolean[] result = new boolean[getColumnSpan()];
		final List<PersistentAttributeMapping> declaredPersistentAttributes = getDeclaredPersistentAttributes();
		for ( int i = 0; i < declaredPersistentAttributes.size(); i++ ) {
			final PersistentAttributeMapping prop = declaredPersistentAttributes.get( i );
			boolean[] chunk = ( (Property) prop ).getValue().getColumnInsertability();
			if ( prop.isInsertable() ) {
				System.arraycopy( chunk, 0, result, i, chunk.length );
			}
			i += chunk.length;
		}
		return result;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		boolean[] result = new boolean[getColumnSpan()];
		final List<PersistentAttributeMapping> attributes = getDeclaredPersistentAttributes();

		for ( int i = 0; i < attributes.size(); i++ ) {
			PersistentAttributeMapping prop = attributes.get( i );
			boolean[] chunk = ( (Property) prop ).getValue().getColumnUpdateability();
			if ( prop.isUpdateable() ) {
				System.arraycopy( chunk, 0, result, i, chunk.length );
			}
			i += chunk.length;
		}
		return result;
	}

	@Override
	public java.util.List<MappedColumn> getMappedColumns() {
		final java.util.List<MappedColumn> columns = new ArrayList<>();
		if ( declaredAttributeMappings != null ) {
			for ( PersistentAttributeMapping p: declaredAttributeMappings.values() ) {
				columns.addAll( p.getValueMapping().getMappedColumns() );
			}
		}
		return columns;
	}
	public boolean isKey() {
		return isKey;
	}

	public void setKey(boolean isKey) {
		this.isKey = isKey;
	}

	public boolean hasPojoRepresentation() {
		return componentClassName != null;
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredPersistentAttribute(String)} instead.
	 */
	@Deprecated
	public Property getProperty(String propertyName) throws MappingException {
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( prop.getName().equals( propertyName ) ) {
				return prop;
			}
		}
		throw new MappingException( "component property not found: " + propertyName );
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"%s( %s : %s )",
				getClass().getSimpleName(),
				getRoleName(),
				getTypeName()
		);
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
		final boolean hasCustomGenerator = !DEFAULT_ID_GEN_STRATEGY.equals( getIdentifierGeneratorStrategy() );
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
		if ( rootClass.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping() != null ) {
			// we have the @IdClass / <composite-id mapped="true"/> case
			attributeDeclarer = resolveComponentClass();
		}
		else if ( rootClass.getIdentifierAttributeMapping() != null ) {
			// we have the "@EmbeddedId" / <composite-id name="idName"/> case
			attributeDeclarer = resolveComponentClass();
		}
		else {
			// we have the "straight up" embedded (again the hibernate term) component identifier
			attributeDeclarer = entityClass;
		}

		locator = new StandardGenerationContextLocator( rootClass.getEntityName() );
		final CompositeNestedGeneratedValueGenerator generator = new CompositeNestedGeneratedValueGenerator( locator );
		List<PersistentAttributeMapping> declaredPersistentAttributes = getDeclaredPersistentAttributes();
		declaredPersistentAttributes.stream()
				.filter( attribute -> SimpleValue.class.isInstance( attribute.getValueMapping()) )
				.forEach( attribute -> {
				final SimpleValue value = (SimpleValue) attribute.getValueMapping();

				// skip any 'assigned' generators, they would have been handled by
				// the StandardGenerationContextLocator
				if ( !DEFAULT_ID_GEN_STRATEGY.equals( value.getIdentifierGeneratorStrategy() ) ) {
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
									injector( (Property) attribute, attributeDeclarer )
							)
					);
				}
		} );
		return generator;
	}

	private Setter injector(Property property, Class attributeDeclarer) {
		return property.getPropertyAccessStrategy( attributeDeclarer )
				.buildPropertyAccess( attributeDeclarer, property.getName() )
				.getSetter();
	}

	private Class resolveComponentClass() {
		try {
			return getComponentClass();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public void addDeclaredPersistentAttribute(PersistentAttributeMapping attribute) {
		if ( declaredAttributeMappings == null ) {
			declaredAttributeMappings = new TreeMap<>();
		}
		else {
			assert !declaredAttributeMappings.containsKey( attribute.getName() );
		}

		declaredAttributeMappings.put( attribute.getName(), attribute );
	}

	@Override
	public void setSuperManagedType(ManagedTypeMapping superTypeMapping) {
		throw new UnsupportedOperationException( "Inheritance not yet supported for composite/embeddable values" );
	}

	public static class StandardGenerationContextLocator
			implements CompositeNestedGeneratedValueGenerator.GenerationContextLocator {
		private final String entityName;

		public StandardGenerationContextLocator(String entityName) {
			this.entityName = entityName;
		}

		@Override
		public Object locateGenerationContext(SharedSessionContractImplementor session, Object incomingObject) {
			return session.getEntityDescriptor( entityName, incomingObject )
					.getHierarchy()
					.getIdentifierDescriptor()
					.extractIdentifier( incomingObject, session );
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
			injector.set( injectionContext, generatedValue, session.getFactory() );
		}

		@Override
		public void registerExportables(Database database) {
			if ( ExportableProducer.class.isInstance( subGenerator ) ) {
				( (ExportableProducer) subGenerator ).registerExportables( database );
			}
		}
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredPersistentAttributes()} instead.
	 */
	@Deprecated
	@Override
	public List<Property> getDeclaredProperties() {
		return declaredAttributeMappings == null
				? Collections.emptyList()
				: declaredAttributeMappings.values().stream().map( p -> (Property) p ).collect( Collectors.toList() );
	}

	@Override
	public List<PersistentAttributeMapping> getDeclaredPersistentAttributes() {
		return declaredAttributeMappings == null
				? Collections.emptyList()
				: new ArrayList<>( declaredAttributeMappings.values() );	}

	@Override
	public List<PersistentAttributeMapping> getPersistentAttributes() {
		List<PersistentAttributeMapping> attributes = new ArrayList<>();
		attributes.addAll( getDeclaredPersistentAttributes() );
		ManagedTypeMapping superTypeMapping = getSuperManagedTypeMapping();
		while ( superTypeMapping != null ) {
			attributes.addAll( superTypeMapping.getPersistentAttributes() );
			superTypeMapping = superTypeMapping.getSuperManagedTypeMapping();
		}
		return attributes;
	}

	@Override
	public PersistentAttributeMapping getDeclaredPersistentAttribute(String attributeName) {
		return declaredAttributeMappings.get( attributeName );
	}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// atm components/embeddables are not (yet) hierarchical

	/**
	 * @deprecated since 6.0, use, use {@link #getSuperManagedTypeMapping()} instead.
	 */
	@Deprecated
	@Override
	public PropertyContainer getSuperPropertyContainer() {
		return null;
	}

	@Override
	public ManagedTypeMapping getSuperManagedTypeMapping() {
		return null;
	}

	@Override
	public List<ManagedTypeMapping> getSuperManagedTypeMappings() {
		return null;
	}

	@Override
	public boolean hasPersistentAttribute(String name) {
		// since we don't support inheritance of embedded components, this delegates to declared
		return hasDeclaredPersistentAttribute( name );
	}

	@Override
	public boolean hasDeclaredPersistentAttribute(String name) {
		for ( PersistentAttributeMapping attribute : getDeclaredPersistentAttributes() ) {
			if ( attribute.getName().equals( name ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	private static EmbeddableJavaDescriptor resolveJavaTypeDescriptor(
			MetadataBuildingContext metadata,
			String componentClassName) {
		final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry = metadata.getMetadataCollector()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry();
		ManagedJavaDescriptor typeDescriptor = (ManagedJavaDescriptor) javaTypeDescriptorRegistry
				.getDescriptor( componentClassName );

		if ( typeDescriptor == null ) {
			final Class javaType;
			if ( StringHelper.isEmpty( componentClassName ) ) {
				javaType = null;
			}
			else {
				javaType = metadata.getBootstrapContext().getServiceRegistry().getService( ClassLoaderService.class )
						.classForName( componentClassName );
			}

			typeDescriptor = new EmbeddableJavaDescriptorImpl( componentClassName, javaType, null );
			javaTypeDescriptorRegistry.addDescriptor( typeDescriptor );
		}
		else if ( !typeDescriptor.isInstance( EmbeddableJavaDescriptor.class ) ) {
			/*
				This may happen with hbm mapping:
				<class name="Entity"...>
					<composite-id>
					 </composite-id>
				</class>
				in such a case the componentClassName is the "Entity" so javaTypeDescriptorRegistry
				.getDescriptor( componentClassName ); is not returning an EmbeddableJavaDescriptor
			 */
			typeDescriptor = new EmbeddableJavaDescriptorImpl( componentClassName, typeDescriptor.getJavaType(), null );
		}
		return (EmbeddableJavaDescriptor) typeDescriptor;
	}
}
