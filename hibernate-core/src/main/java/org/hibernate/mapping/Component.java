/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator.GenerationPlan;
import org.hibernate.id.Configurable;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.Creator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyGetterImpl;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.UserComponentType;
import org.hibernate.type.MappingContext;
import org.hibernate.usertype.CompositeUserType;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.mapping.MappingHelper.checkPropertyColumnDuplication;
import static org.hibernate.mapping.MappingHelper.createCompositeUserType;

/**
 * A mapping model object that represents an {@linkplain jakarta.persistence.Embeddable embeddable class}.
 *
 * @apiNote The name of this class is historical and unfortunate. It reflects modeling a *composition*
 *          of state. It has absolutely nothing to do with modularity in software engineering.
 *          A component remains {@link ComponentShapeState#BUILDING} while its properties and
 *          ordering policy are being contributed. Positional consumers of its properties,
 *          columns, or selectables must only use it after {@link #completeShape()}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Component extends SimpleValue implements AttributeContainer, MetaAttributable, SortableValue {

	private ClassDetails componentClassDetails;
	private boolean flattened;
	private String parentProperty;
	private PersistentClass owner;
	private boolean isKey;
	private transient Boolean isGeneric;
	private transient CompositeUserType<?> compositeUserType;
	private String roleName;
	private MappedSuperclass mappedSuperclass;
	private Value discriminator;
	private transient DiscriminatorType<?> discriminatorType;
	private Map<Object, String> discriminatorValues;
	private Map<String, String> subclassToSuperclass;

	private final ArrayList<Property> properties = new ArrayList<>();
	private ComponentShapeState shapeState = ComponentShapeState.BUILDING;
	private Map<Property, String> propertyDeclaringClasses;
	private int[] originalPropertyOrder = ArrayHelper.EMPTY_INT_ARRAY;
	private Map<String,MetaAttribute> metaAttributes;

	private Class<? extends EmbeddableInstantiator> customInstantiator;
	private transient Constructor<?> instantiator;
	private String[] instantiatorPropertyNames;

	// cache the status of the type
	private transient volatile CompositeType type;

	private AggregateColumn aggregateColumn;
	private AggregateColumn parentAggregateColumn;
	private QualifiedName structName;
	private String[] structColumnNames;
	private transient Boolean simpleRecord;
	private boolean preservePropertyOrder;
	private String columnNamingPattern;

	private boolean tableWasExplicit;

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
		this.componentClassDetails = original.componentClassDetails;
		this.flattened = original.flattened;
		this.parentProperty = original.parentProperty;
		this.owner = original.owner;
		this.isGeneric = original.isGeneric;
		this.compositeUserType = original.compositeUserType;
		this.metaAttributes = original.metaAttributes == null ? null : new HashMap<>( original.metaAttributes );
		this.isKey = original.isKey;
		this.roleName = original.roleName;
		this.discriminator = original.discriminator;
		this.discriminatorType = original.discriminatorType;
		this.discriminatorValues = original.discriminatorValues;
		this.subclassToSuperclass = original.subclassToSuperclass;
		this.customInstantiator = original.customInstantiator;
		this.type = original.type;
	}

	@Override
	public Component copy() {
		return new Component( this );
	}

	/// Creates a declaration-side component projection from the supplied
	/// roleless properties.
	///
	/// The returned component has no [#getMappingRole()], and every supplied
	/// property must likewise have no mapping role.  The component's
	/// discriminator, when present, is copied as roleless declarative state.
	/// Polymorphic property-declaration origin is rebuilt by matching the
	/// supplied properties to the source properties by position.
	///
	/// The normal use is an independent generic embeddable declaration
	/// template:
	///
	/// ```java
	/// List<Property> declarations = appliedComponent.getProperties()
	///         .stream()
	///         .map( property -> property.copyForDeclaration(
	///                 property.getValue().copy()
	///         ) )
	///         .toList();
	///
	/// Component declaration =
	///         appliedComponent.copyForDeclaration( declarations );
	///
	/// assert declaration.getMappingRole() == null;
	/// assert declaration.getProperties().stream()
	///         .noneMatch( p -> p.getMappingRole() != null );
	/// ```
	///
	/// This is used for generic embeddable templates and generic
	/// mapped-superclass component properties.  It also builds the
	/// mapped-superclass declaration view of an identifier mapper.  That legacy
	/// case supplies [Property#copyForDeclarationView() declaration views]
	/// whose properties are roleless but whose values remain shared with the
	/// applied identifier mapping.
	///
	/// This method creates a declaration container, not a filtered component or
	/// a new concrete occurrence.  Use
	/// [#copyForPropertySelection(List)] for transient column matching,
	/// [#copyForSameApplication()] for an alias of the current occurrence, or
	/// [#copyForApplication(MetadataBuildingContext, MappingRole, List)] for a
	/// distinct application in the same physical mapping context.
	///
	/// @param declarationProperties roleless declaration-side properties, in
	/// source property order
	/// @return a roleless component containing the supplied properties
	/// @throws NullPointerException if `declarationProperties` is `null`
	/// @throws IllegalArgumentException if a supplied property has a mapping role
	public Component copyForDeclaration(List<Property> declarationProperties) {
		Objects.requireNonNull( declarationProperties, "Declaration properties" );
		final Component copy = new Component( this );
		copy.clearProperties();
		copy.propertyDeclaringClasses = null;
		if ( discriminator != null ) {
			final Value declarationDiscriminator = discriminator.copy();
			if ( declarationDiscriminator instanceof AppliedMappingPart mappingPart
					&& mappingPart.getMappingRole() != null ) {
				throw new IllegalArgumentException(
						"Declaration discriminator already has a mapping role: " + mappingPart.getMappingRole()
				);
			}
			copy.discriminator = declarationDiscriminator;
		}
		for ( int i = 0; i < declarationProperties.size(); i++ ) {
			final Property declarationProperty = declarationProperties.get( i );
			if ( declarationProperty.getMappingRole() != null ) {
				throw new IllegalArgumentException(
						"Declaration property already has a mapping role: " + declarationProperty.getMappingRole()
				);
			}
			copy.properties.add( declarationProperty );
			if ( propertyDeclaringClasses != null && i < properties.size() ) {
				final String declaringClass = propertyDeclaringClasses.get( properties.get( i ) );
				if ( declaringClass != null ) {
					if ( copy.propertyDeclaringClasses == null ) {
						copy.propertyDeclaringClasses = new HashMap<>();
					}
					copy.propertyDeclaringClasses.put( declarationProperty, declaringClass );
				}
			}
		}
		return copy;
	}

	/// Creates a transient, roleless view over selected properties of this
	/// component.
	///
	/// Both the `Property` objects and their values remain shared with the source
	/// component.  The component discriminator is omitted and source property
	/// order is preserved:
	///
	/// ```java
	/// List<Property> matching = selectPropertiesByReferencedColumns(
	///         component,
	///         joinColumns
	/// );
	/// Component selection =
	///         component.copyForPropertySelection( matching );
	///
	/// assert selection.getMappingRole() == null;
	/// assert selection.getProperties().get( 0 ) == matching.get( 0 );
	/// ```
	///
	/// This view is used while resolving a composite association target from
	/// `referencedColumnName` values.  The association binder may need to treat
	/// a subset of an embeddable's properties as one temporary value solely to
	/// compare column order and span.
	///
	/// The result is neither declaration state nor an applied mapping.  It must
	/// not be registered, installed as a property's value, or otherwise
	/// attached to the boot mapping graph.  Use [#copyForDeclaration(List)] or
	/// [#copyForApplication(MetadataBuildingContext, MappingRole, List)] when a
	/// durable mapping object is required.
	///
	/// @param selectedProperties source properties selected for matching, in the
	/// required comparison order
	/// @return a transient roleless component sharing the selected properties
	/// @throws NullPointerException if `selectedProperties` is `null`
	public Component copyForPropertySelection(List<Property> selectedProperties) {
		Objects.requireNonNull( selectedProperties, "Selected properties" );
		final Component copy = new Component( this );
		copy.clearProperties();
		copy.propertyDeclaringClasses = null;
		copy.discriminator = null;
		copy.discriminatorType = null;
		copy.discriminatorValues = null;
		copy.preservePropertyOrder = true;
		for ( Property selectedProperty : selectedProperties ) {
			copy.properties.add( selectedProperty );
			if ( propertyDeclaringClasses != null ) {
				final String declaringClass = propertyDeclaringClasses.get( selectedProperty );
				if ( declaringClass != null ) {
					if ( copy.propertyDeclaringClasses == null ) {
						copy.propertyDeclaringClasses = new HashMap<>();
					}
					copy.propertyDeclaringClasses.put( selectedProperty, declaringClass );
				}
			}
		}
		return copy;
	}

	/// Creates another compatibility projection of the **same** concrete
	/// component application.
	///
	/// The component role, child `Property` objects, their values, and the
	/// discriminator are shared with the source occurrence:
	///
	/// ```java
	/// Component alias = identifierComponent.copyForSameApplication();
	///
	/// assert alias != identifierComponent;
	/// assert alias.getMappingRole()
	///         .equals( identifierComponent.getMappingRole() );
	/// assert alias.getProperties().get( 0 )
	///         == identifierComponent.getProperties().get( 0 );
	/// ```
	///
	/// The primary use is an identifier-mapper compatibility projection which
	/// exposes an existing identifier component in another legacy shape.  It is
	/// not a second identifier mapping: retaining the original identifier and
	/// nested-property roles is therefore required.
	///
	/// Use this operation only when late changes to the original properties or
	/// values must be visible through the copy.  A component which represents a
	/// genuinely new occurrence needs independently materialized child
	/// properties and [#copyForApplication(MetadataBuildingContext, MappingRole,
	/// List)].
	///
	/// @return a component alias retaining this component's role and children
	public Component copyForSameApplication() {
		final Component copy = new Component( this );
		copy.setMappingRole( getMappingRole() );
		return copy;
	}

	/// Creates a component for a **new** concrete application in the same
	/// physical table and owner context as this component.
	///
	/// The caller supplies independently-materialized child properties.  Each
	/// child must either be roleless or already have the role obtained by
	/// appending its attribute name to `mappingRole`.  Roleless children are
	/// assigned that role while they are installed:
	///
	/// ```java
	/// MappingRole workAddressRole =
	///         MappingRole.entity( Customer.class.getName() )
	///                 .appendAttribute( "workAddress" );
	///
	/// List<Property> workAddressProperties = addressDeclaration.getProperties()
	///         .stream()
	///         .map( property -> {
	///             MappingRole propertyRole =
	///                     workAddressRole.appendAttribute( property.getName() );
	///             return property.copyForApplication(
	///                     propertyRole,
	///                     copyValueForApplication(
	///                             property.getValue(),
	///                             propertyRole
	///                     )
	///             );
	///         } )
	///         .toList();
	///
	/// Component workAddress = addressDeclaration.copyForApplication(
	///         buildingContext,
	///         workAddressRole,
	///         workAddressProperties
	/// );
	/// ```
	///
	/// This operation is used when a generic, component-valued property declared
	/// by an entity superclass is specialized for a concrete entity subclass.
	/// More generally, it is appropriate for a repeated embeddable or component
	/// projection when the new occurrence retains the source component's
	/// physical owner and table.  The component and children have independent
	/// application identities and value state; this is unlike an identifier
	/// mapper or synthetic alias built with [#copyForSameApplication()].
	///
	/// Most primary embeddable mappings are materialized directly from an
	/// `EmbeddableContribution` instead of copied.  Component-valued collection
	/// indexes are also new applications, but they are constructed afresh
	/// because their physical collection table or owner context differs from
	/// the source component.  This method must not be used for such a physical
	/// relocation.
	///
	/// The copied component is registered with the metadata collector.  Its
	/// discriminator, if any, is independently copied and assigned a
	/// discriminator role below `mappingRole`.
	///
	/// @param buildingContext context whose metadata collector will own the copy
	/// @param mappingRole stable identity of the new component occurrence
	/// @param applicationProperties independently-materialized direct properties
	/// in source property order
	/// @return a registered component with the supplied application identity
	/// @throws NullPointerException if an argument is `null`
	/// @throws IllegalArgumentException if a property does not correspond to the
	/// source component or belongs to another application
	public Component copyForApplication(
			MetadataBuildingContext buildingContext,
			MappingRole mappingRole,
			List<Property> applicationProperties) {
		Objects.requireNonNull( buildingContext, "Metadata building context" );
		Objects.requireNonNull( mappingRole, "Mapping role" );
		Objects.requireNonNull( applicationProperties, "Application properties" );
		if ( applicationProperties.size() != properties.size() ) {
			throw new IllegalArgumentException(
					"Application property count does not match source component: expected "
							+ properties.size() + ", got " + applicationProperties.size()
			);
		}

		final Component copy = new Component( this );
		copy.clearProperties();
		copy.propertyDeclaringClasses = null;
		copy.setMappingRole( mappingRole );
		if ( discriminator != null ) {
			final Value applicationDiscriminator = discriminator.copy();
			if ( applicationDiscriminator instanceof AppliedMappingPart mappingPart ) {
				final MappingRole discriminatorRole = mappingRole.append( MappingRole.PartKind.DISCRIMINATOR );
				if ( mappingPart.getMappingRole() != null
						&& !discriminatorRole.equals( mappingPart.getMappingRole() ) ) {
					throw new IllegalArgumentException(
							"Application discriminator has a different mapping role: "
									+ mappingPart.getMappingRole()
					);
				}
				mappingPart.setMappingRole( discriminatorRole );
			}
			copy.discriminator = applicationDiscriminator;
		}
		for ( int i = 0; i < applicationProperties.size(); i++ ) {
			final Property sourceProperty = properties.get( i );
			final Property applicationProperty = applicationProperties.get( i );
			if ( !sourceProperty.getName().equals( applicationProperty.getName() ) ) {
				throw new IllegalArgumentException(
						"Application property at position " + i + " does not match source property '"
								+ sourceProperty.getName() + "': " + applicationProperty.getName()
				);
			}
			final MappingRole propertyRole = mappingRole.appendAttribute( applicationProperty.getName() );
			if ( applicationProperty.getMappingRole() != null
					&& !propertyRole.equals( applicationProperty.getMappingRole() ) ) {
				throw new IllegalArgumentException(
						"Application property '" + applicationProperty.getName()
								+ "' has a different mapping role: " + applicationProperty.getMappingRole()
				);
			}
			if ( applicationProperty.getValue() instanceof AppliedMappingPart mappingPart
					&& mappingPart.getMappingRole() != null
					&& !propertyRole.equals( mappingPart.getMappingRole() ) ) {
				throw new IllegalArgumentException(
						"Application property value '" + applicationProperty.getName()
								+ "' has a different mapping role: " + mappingPart.getMappingRole()
				);
			}
			applicationProperty.setMappingRole( propertyRole );
			copy.properties.add( applicationProperty );
			if ( propertyDeclaringClasses != null ) {
				final String declaringClass = propertyDeclaringClasses.get( sourceProperty );
				if ( declaringClass != null ) {
					if ( copy.propertyDeclaringClasses == null ) {
						copy.propertyDeclaringClasses = new HashMap<>();
					}
					copy.propertyDeclaringClasses.put( applicationProperty, declaringClass );
				}
			}
		}
		buildingContext.getMetadataCollector().registerComponent( copy );
		return copy;
	}

	public int getPropertySpan() {
		return properties.size();
	}

	public List<Property> getProperties() {
		return unmodifiableList( properties );
	}

	public void setTable(Table table) {
		if ( !tableWasExplicit ) {
			super.setTable( table );
		}

		// otherwise, ignore it...
	}

	public void setTable(Table table, boolean wasExplicit) {
		super.setTable( table );
		tableWasExplicit = wasExplicit;
	}

	public boolean wasTableExplicitlyDefined() {
		return tableWasExplicit;
	}

	public void addProperty(Property p, ClassDetails declaringClass) {
		requireShapeBuilding();
		properties.add( p );
		applyPropertyMappingRole( p );
		if ( isPolymorphic() && declaringClass != null ) {
			if ( propertyDeclaringClasses == null ) {
				propertyDeclaringClasses = new HashMap<>();
			}
			propertyDeclaringClasses.put( p, declaringClass.getClassName() == null
					? declaringClass.getName()
					: declaringClass.getClassName() );
		}
	}

	@Override
	public void addProperty(Property p) {
		addProperty( p, null );
	}

	@Override
	public void setMappingRole(MappingRole mappingRole) {
		super.setMappingRole( mappingRole );
		if ( mappingRole != null ) {
			for ( Property property : properties ) {
				applyPropertyMappingRole( property );
			}
		}
	}

	private void applyPropertyMappingRole(Property property) {
		if ( getMappingRole() != null && property.getMappingRole() == null ) {
			property.setMappingRole( getMappingRole().appendAttribute( property.getName() ) );
		}
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
		for ( var property : properties ) {
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
		for ( var property : properties ) {
			columns.addAll( property.getValue().getColumns() );
		}
		if ( discriminator != null ) {
			columns.addAll( discriminator.getColumns() );
		}
		return unmodifiableList( columns );
	}

	/**
	 * Whether this component is flattened into its owning managed type.
	 * <p>
	 * This is Hibernate's historical "embedded" component mode, used for
	 * non-aggregated composite identifiers and identifier mappers where the
	 * component's properties are exposed as properties of the owning type.
	 */
	public boolean isFlattened() {
		return flattened;
	}

	/**
	 * @deprecated Use {@link #isFlattened()}.  This method name reflects
	 * historical Hibernate terminology and does not mean the same as JPA's
	 * {@code @Embedded}.
	 */
	@Deprecated(since = "9.0")
	public boolean isEmbedded() {
		return isFlattened();
	}

	public AggregateColumn getAggregateColumn() {
		return aggregateColumn;
	}

	public void setAggregateColumn(AggregateColumn aggregateColumn) {
		this.aggregateColumn = aggregateColumn;
		this.type = null;
		notifyPropertiesAboutAggregateColumn( aggregateColumn, this );
	}

	@Override
	public int getColumnSpan() {
		return getSelectables().size();
	}

	@Override
	public boolean hasColumns() {
		for ( var property : properties ) {
			if ( property.hasColumns() ) {
				return true;
			}
		}
		return discriminator != null
			&& discriminator.hasColumns();
	}

	public List<Column> getAggregatedColumns() {
		final List<Column> aggregatedColumns = new ArrayList<>( getPropertySpan() );
		collectAggregatedColumns( aggregatedColumns, this );
		return aggregatedColumns;
	}

	private void collectAggregatedColumns(List<Column> aggregatedColumns, Component component) {
		for ( var property : component.getProperties() ) {
			if ( property.getValue() instanceof Component subComponent ) {
				final var subAggregate = subComponent.getAggregateColumn();
				if ( subAggregate != null ) {
					aggregatedColumns.add( subAggregate );
				}
				else {
					collectAggregatedColumns( aggregatedColumns, subComponent );
				}
			}
			else {
				aggregatedColumns.addAll( property.getValue().getColumns() );
			}
		}
		if ( component.isPolymorphic() ) {
			aggregatedColumns.addAll( component.getDiscriminator().getColumns() );
		}
	}

	private void notifyPropertiesAboutAggregateColumn(AggregateColumn aggregateColumn, Component component) {
		for ( var property : component.getProperties() ) {
			// Let the BasicValue of every sub-column know about the aggregate,
			// which is needed in type resolution
			final var value = property.getValue();
			if ( value instanceof BasicValue basicValue ) {
				assert basicValue.getResolution() == null;
				basicValue.setAggregateColumn( aggregateColumn );
			}
			else if ( value instanceof Component subComponent ) {
				if ( subComponent.getAggregateColumn() == null ) {
					subComponent.notifyPropertiesAboutAggregateColumn( aggregateColumn, subComponent );
				}
				else {
					subComponent.setParentAggregateColumn( aggregateColumn );
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
	public void checkColumnDuplication(Set<QualifiedColumnName> distinctColumns, String owner, Database database) {
		if ( aggregateColumn == null ) {
			if ( isPolymorphic() ) {
				// We can allow different subtypes reusing the same columns
				// since only one subtype can exist at one time
				final Map<String, Set<QualifiedColumnName>> distinctColumnsByClass = new HashMap<>();
				for ( var prop : properties ) {
					if ( prop.isUpdatable() || prop.isInsertable() ) {
						final String declaringClass = propertyDeclaringClasses.get( prop );
						final Set<QualifiedColumnName> set = distinctColumnsByClass.computeIfAbsent(
								declaringClass,
								k -> new HashSet<>( distinctColumns )
						);
						prop.getValue().checkColumnDuplication( set, owner, database );
					}
				}
				for ( var columns : distinctColumnsByClass.values() ) {
					distinctColumns.addAll( columns );
				}
			}
			else {
				checkPropertyColumnDuplication( distinctColumns, getProperties(), owner, database );
			}
		}
		else {
			checkPropertyColumnDuplication( new HashSet<>(), getProperties(), "component '" + getRoleName() + "'", database );
			aggregateColumn.getValue().checkColumnDuplication( distinctColumns, owner, database );
		}
	}

	public String getComponentClassName() {
		if ( componentClassDetails == null ) {
			return null;
		}
		final String className = componentClassDetails.getClassName();
		return className == null ? componentClassDetails.getName() : className;
	}

	public ClassDetails getComponentClassDetails() {
		return componentClassDetails;
	}

	public Class<?> getComponentClass() throws MappingException {
		if ( componentClassDetails == null || !componentClassDetails.isRealClass() ) {
			return null;
		}
		try {
			return componentClassDetails.toJavaClass();
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "Embeddable class not found: " + getComponentClassName(), e );
		}
	}

	public PersistentClass getOwner() {
		return owner;
	}

	public String getParentProperty() {
		return parentProperty;
	}

	public void setComponentClassDetails(ClassDetails componentClassDetails) {
		requireShapeBuilding();
		this.componentClassDetails = componentClassDetails;
		this.simpleRecord = null;
		if ( componentClassDetails != null && !componentClassDetails.isRealClass() ) {
			this.isGeneric = false;
			this.roleName = componentClassDetails.getName();
		}
	}

	public void setComponentClassDetails(String name, boolean dynamic, MetadataBuildingContext buildingContext) {
		final var modelsContext = buildingContext.getBootstrapContext().getModelsContext();
		final var deets = dynamic
				? Creator.createDynamicClassDetails( name, modelsContext )
				: modelsContext.getClassDetailsRegistry().resolveClassDetails( name );
		setComponentClassDetails( deets );
	}

	public void setFlattened(boolean flattened) {
		requireShapeBuilding();
		this.flattened = flattened;
	}

	/**
	 * @deprecated Use {@link #setFlattened(boolean)}.  This method name reflects
	 * historical Hibernate terminology and does not mean "mapped by
	 * {@code @Embedded}".
	 */
	@Deprecated(since = "9.0", forRemoval = true)
	public void setEmbedded(boolean flattened) {
		setFlattened( flattened );
	}

	public void setOwner(PersistentClass owner) {
		this.owner = owner;
	}

	public void setParentProperty(String parentProperty) {
		this.parentProperty = parentProperty;
	}

	public boolean isDynamic() {
		return componentClassDetails != null && !componentClassDetails.isRealClass();
	}

	public CompositeUserType<?> getCompositeUserType() {
		return compositeUserType;
	}

	public void setCompositeUserType(CompositeUserType<?> compositeUserType) {
		this.compositeUserType = compositeUserType;
		setTypeName( compositeUserType == null ? null : compositeUserType.getClass().getName() );
		this.type = null;
	}

	/**
	 * Recreates the transient custom-type instance and the property-access
	 * strategy derived from it after this component has been restored.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void reattachCompositeUserType(
			ClassLoaderAccess classLoaderAccess,
			ManagedBeanRegistry managedBeanRegistry,
			boolean allowExtensionsInCdi) {
		if ( getTypeName() == null ) {
			return;
		}
		final Class<? extends CompositeUserType<?>> compositeUserTypeClass =
				(Class<? extends CompositeUserType<?>>) (Class<?>) classLoaderAccess.classForName( getTypeName() );
		compositeUserType = createCompositeUserType(
				compositeUserTypeClass,
				managedBeanRegistry,
				allowExtensionsInCdi
		);
		final List<String> propertyNames = new ArrayList<>( getPropertySpan() );
		final List<java.lang.reflect.Type> propertyTypes = new ArrayList<>( getPropertySpan() );
		final var strategy = new PropertyAccessStrategyCompositeUserTypeImpl(
				compositeUserType,
				propertyNames,
				propertyTypes
		);
		for ( Property property : properties ) {
			propertyNames.add( property.getName() );
			propertyTypes.add(
					PropertyAccessStrategyGetterImpl.INSTANCE.buildPropertyAccess(
							compositeUserType.embeddable(),
							property.getName(),
							false
					).getGetter().getReturnType()
			);
			property.setPropertyAccessStrategy( strategy );
		}
	}

	@Override
	public boolean contains(Property property) {
		return properties.contains( property );
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
					// Make sure the shape is complete, which is important especially for
					// synthetic components. Other components should already be complete.
					completeShape( true );

					if ( compositeUserType == null ) {
						localType = isFlattened()
								? new EmbeddedComponentType( this, originalPropertyOrder )
								: new ComponentType( this, originalPropertyOrder );
					}
					else {
						localType = new UserComponentType<>( this, originalPropertyOrder, compositeUserType );
					}

					type = localType;
				}
			}
		}

		return localType;
	}

	@Override
	public void setTypeUsingReflection(
			String className,
			String propertyName,
			MetadataBuildingContext buildingContext)
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
		return other instanceof Component component && isSame( component );
	}

	public boolean isSame(Component other) {
		return super.isSame( other )
			&& Objects.equals( properties, other.properties )
			&& Objects.equals( getComponentClassName(), other.getComponentClassName() )
			&& flattened == other.flattened
			&& Objects.equals( aggregateColumn, other.aggregateColumn )
			&& Objects.equals( parentAggregateColumn, other.parentAggregateColumn )
			&& Objects.equals( structName, other.structName )
			&& Objects.equals( parentProperty, other.parentProperty )
			&& Objects.equals( metaAttributes, other.metaAttributes );
	}

	@Override
	public boolean[] getColumnInsertability() {
		final var result = new boolean[getColumnSpan()];
		int i = 0;
		for ( var property : getProperties() ) {
			i += copyFlags( property.getValue().getColumnInsertability(), result, i, property.isInsertable() );
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
		for ( var property : properties ) {
			if ( property.getValue().hasAnyInsertableColumns() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		final var result = new boolean[getColumnSpan()];
		int i = 0;
		for ( var property : getProperties() ) {
			i += copyFlags( property.getValue().getColumnUpdateability(), result, i, property.isUpdatable() );
		}
		if ( isPolymorphic() ) {
			i += copyFlags( getDiscriminator().getColumnUpdateability(), result, i, true );
		}
		assert i == getColumnSpan();
		return result;
	}

	@Override
	public void setNonUpdatable() {
		for ( var property : properties ) {
			property.getValue().setNonUpdatable();
		}
		if ( isPolymorphic() ) {
			getDiscriminator().setNonUpdatable();
		}
	}

	@Override
	public void setNonInsertable() {
		for ( var property : properties ) {
			property.getValue().setNonInsertable();
		}
		if ( isPolymorphic() ) {
			getDiscriminator().setNonInsertable();
		}
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		for ( var property : properties ) {
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

	@Override
	public Property getProperty(String propertyName) throws MappingException {
		for ( var property : properties ) {
			if ( property.getName().equals(propertyName) ) {
				return property;
			}
		}
		throw new MappingException("component: " + getComponentClassName() + " property not found: " + propertyName);
	}

	public boolean matchesAllProperties(String... propertyNames) {
		return properties.size() == propertyNames.length &&
				new HashSet<>(properties.stream().map(Property::getName)
						.collect(toList()))
						.containsAll(List.of(propertyNames));
	}

	public boolean hasProperty(String propertyName) {
		for ( var property : properties ) {
			if ( property.getName().equals(propertyName) ) {
				return true;
			}
		}
		return false;
	}

	public String getRoleName() {
		return roleName != null
				? roleName
				: getMappingRole() == null ? null : getMappingRole().getFullPath();
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
		requireShapeBuilding();
		this.discriminator = discriminator;
	}

	public DiscriminatorType<?> getDiscriminatorType() {
		return discriminatorType;
	}

	public void setDiscriminatorType(DiscriminatorType<?> discriminatorType) {
		requireShapeBuilding();
		this.discriminatorType = discriminatorType;
	}

	public void reattachDiscriminatorType(DiscriminatorType<?> discriminatorType) {
		this.discriminatorType = discriminatorType;
	}

	public boolean isPolymorphic() {
		return discriminator != null;
	}

	public Map<Object, String> getDiscriminatorValues() {
		return discriminatorValues;
	}

	public void setDiscriminatorValues(Map<Object, String> discriminatorValues) {
		requireShapeBuilding();
		this.discriminatorValues = discriminatorValues;
	}

	public String getSuperclass(String subclass) {
		return subclassToSuperclass.get( subclass );
	}

	public Map<String, String> getSubclassToSuperclass() {
		return subclassToSuperclass;
	}

	public void setSubclassToSuperclass(Map<String, String> subclassToSuperclass) {
		this.subclassToSuperclass = subclassToSuperclass;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + getComponentClassName() + ')';
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
		requireShapeBuilding();
		properties.clear();
	}

	public void replaceProperties(Function<Property, Property> replacement) {
		requireShapeBuilding();
		properties.replaceAll( replacement::apply );
	}

	public void removePropertiesIf(Predicate<Property> predicate) {
		requireShapeBuilding();
		properties.removeIf( predicate );
	}

	/**
	 * Apply a column naming pattern.
	 *
	 * @see org.hibernate.annotations.EmbeddedColumnNaming
	 */
	public void setColumnNamingPattern(String columnNamingPattern) {
		this.columnNamingPattern = columnNamingPattern;
	}

	/**
	 * Column naming pattern applied to the component
	 *
	 * @see org.hibernate.annotations.EmbeddedColumnNaming
	 */
	public String getColumnNamingPattern() {
		return columnNamingPattern;
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

	public static class ValueGenerationPlan implements GenerationPlan {
		private final BeforeExecutionGenerator generator;
		private final Setter injector;
		private final int propertyIndex;

		public ValueGenerationPlan(BeforeExecutionGenerator generator, Setter injector, int propertyIndex) {
			this.generator = generator;
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
			return generator;
		}

		@Override
		public void registerExportables(Database database) {
			if ( generator instanceof ExportableProducer exportableProducer ) {
				exportableProducer.registerExportables( database );
			}
		}

		@Override
		public void initialize(SqlStringGenerationContext context) {
			if ( generator instanceof Configurable configurable ) {
				configurable.initialize( context );
			}
		}
	}

	public void prepareForMappingModel() {
		// This call will initialize the type properly
		getType();
	}

	@Override
	public boolean isValid(MappingContext mappingContext) throws MappingException {
		if ( !super.isValid( mappingContext ) ) {
			return false;
		}
		if ( instantiatorPropertyNames != null ) {
			if ( instantiatorPropertyNames.length < properties.size() ) {
				throw new MappingException( "component type [" + getComponentClassName() + "] specifies " + instantiatorPropertyNames.length + " properties for the instantiator but has " + properties.size() + " properties" );
			}
			final HashSet<String> assignedPropertyNames = CollectionHelper.setOfSize( properties.size() );
			for ( String instantiatorPropertyName : instantiatorPropertyNames ) {
				if ( getProperty( instantiatorPropertyName ) == null ) {
					throw new MappingException( "could not find property [" + instantiatorPropertyName + "] defined in the @Instantiator withing component [" + getComponentClassName() + "]" );
				}
				assignedPropertyNames.add( instantiatorPropertyName );
			}
			if ( assignedPropertyNames.size() != properties.size() ) {
				final ArrayList<String> missingProperties = new ArrayList<>();
				for ( var property : properties ) {
					final String propertyName = property.getName();
					if ( !assignedPropertyNames.contains( propertyName ) ) {
						missingProperties.add( propertyName );
					}
				}
				throw new MappingException( "component type [" + getComponentClassName() + "] has " + properties.size() + " properties but the instantiator only assigns " + assignedPropertyNames.size() + " properties. missing properties: " + missingProperties );
			}
		}
		return true;
	}

	@Override
	public boolean isSorted() {
		return shapeState == ComponentShapeState.COMPLETE;
	}

	@Override
	public int[] sortProperties(Function<String, PersistentClass> entityBindingResolver) {
		return completeShape();
	}

	/**
	 * Complete this component's structural shape, choosing its final property
	 * order and, where necessary, retaining the correspondence with the order
	 * in which its properties were contributed.
	 *
	 * @return the original-to-completed property-order correspondence, or
	 * {@code null} when no correspondence is needed
	 */
	public int[] completeShape() {
		return completeShape( false );
	}

	private int[] completeShape(boolean forceRetainOriginalOrder) {
		if ( shapeState == ComponentShapeState.COMPLETE ) {
			return originalPropertyOrder;
		}
		if ( preservePropertyOrder ) {
			originalPropertyOrder = null;
			shapeState = ComponentShapeState.COMPLETE;
			return null;
		}
		// Don't sort the properties for a simple record
		if ( isSimpleRecord() ) {
			originalPropertyOrder = null;
			shapeState = ComponentShapeState.COMPLETE;
			return null;
		}
		final int[] originalPropertyOrder;
		// We need to capture the original property order if this is an alternate unique key or embedded component property
		// to be able to sort the other side of the foreign key accordingly
		// and also if the source is a XML mapping
		// because XML mappings might refer to this through the defined order
		if ( forceRetainOriginalOrder || isAlternateUniqueKey() || isFlattened() ) {
			final var originalProperties = properties.toArray( new Property[0] );
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
		this.originalPropertyOrder = originalPropertyOrder;
		shapeState = ComponentShapeState.COMPLETE;
		return originalPropertyOrder;
	}

	public ComponentShapeState getShapeState() {
		return shapeState;
	}

	public boolean isShapeComplete() {
		return shapeState == ComponentShapeState.COMPLETE;
	}

	private void requireShapeBuilding() {
		if ( shapeState != ComponentShapeState.BUILDING ) {
			throw new IllegalStateException( "Component shape is already complete: " + this );
		}
	}

	/**
	 * Require this component's structural shape to be complete before performing
	 * an operation which depends on the positional order of its properties,
	 * columns, or selectables.
	 */
	public void requireShapeComplete() {
		if ( shapeState != ComponentShapeState.COMPLETE ) {
			throw new IllegalStateException( "Component shape is not complete: " + this );
		}
	}

	public void setSimpleRecord(boolean simpleRecord) {
		requireShapeBuilding();
		this.simpleRecord = simpleRecord;
	}

	public boolean isSimpleRecord() {
		Boolean simple = simpleRecord;
		if ( simple == null ) {
			// A simple record is given, when the properties match the order of the record component names
			final var componentClass = resolveComponentClass();
			if ( customInstantiator != null ) {
				return simpleRecord = false;
			}
			if ( componentClass == null || !componentClass.isRecord() ) {
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

	public boolean isPreservePropertyOrder() {
		return preservePropertyOrder;
	}

	public void setPreservePropertyOrder(boolean preservePropertyOrder) {
		requireShapeBuilding();
		this.preservePropertyOrder = preservePropertyOrder;
	}

	private Class<?> resolveComponentClass() {
		try {
			return getComponentClass();
		}
		catch ( Exception e ) {
			return null;
		}
	}

	public Class<? extends EmbeddableInstantiator> getCustomInstantiator() {
		return customInstantiator;
	}

	public void setCustomInstantiator(Class<? extends EmbeddableInstantiator> customInstantiator) {
		requireShapeBuilding();
		this.customInstantiator = customInstantiator;
	}

	public Constructor<?> getInstantiator() {
		return instantiator;
	}

	public String[] getInstantiatorPropertyNames() {
		return instantiatorPropertyNames;
	}

	public void setInstantiator(Constructor<?> instantiator, String[] instantiatorPropertyNames) {
		requireShapeBuilding();
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
			final Class<?> componentClass = getComponentClass();
			isGeneric = componentClass != null && componentClass.getTypeParameters().length > 0;
		}
		return isGeneric;
	}

	public void setGeneric(boolean generic) {
		isGeneric = generic;
	}

	public boolean isRecord() {
		return getComponentClass() != null && getComponentClass().isRecord();
	}
}
