/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import static org.hibernate.boot.model.internal.ClassPropertyHolder.addPropertyToMappedSuperclass;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.model.internal.HCANNHelper.hasAnnotation;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualifyConditionally;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

/**
 * {@link PropertyHolder} for composites (Embeddable/Embedded).
 * <p>
 * To facilitate code comments, I'll often refer to this example:
 * <pre>
 *     &#064;Embeddable
 *     &#064;Convert( attributeName="city", ... )
 *     class Address {
 *         ...
 *         &#064;Convert(...)
 *         public String city;
 *     }
 *
 *     &#064;Entity
 *     &#064;Convert( attributeName="homeAddress.city", ... )
 *     class Person {
 *         ...
 *         &#064;Embedded
 *         &#064;Convert( attributeName="city", ... )
 *         public Address homeAddress;
 *     }
 * </pre>
 * <p>
 * As you can see, lots of ways to specify the conversion for embeddable attributes :(
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class ComponentPropertyHolder extends AbstractPropertyHolder {
	private final Component component;
	private final boolean isOrWithinEmbeddedId;
	private final boolean isWithinElementCollection;
	private final Map<XClass, InheritanceState> inheritanceStatePerClass;

	private final String embeddedAttributeName;
	private final Map<String,AttributeConversionInfo> attributeConversionInfoMap;

	public ComponentPropertyHolder(
			Component component,
			String path,
			PropertyData inferredData,
			PropertyHolder parent,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		super( path, parent, inferredData.getPropertyClass(), context );
		final XProperty embeddedXProperty = inferredData.getProperty();
		setCurrentProperty( embeddedXProperty );
		this.component = component;
		this.isOrWithinEmbeddedId = parent.isOrWithinEmbeddedId()
				|| hasAnnotation( embeddedXProperty, Id.class, EmbeddedId.class );
		this.isWithinElementCollection = parent.isWithinElementCollection() ||
			parent instanceof CollectionPropertyHolder;
		this.inheritanceStatePerClass = inheritanceStatePerClass;

		if ( embeddedXProperty != null ) {
			this.embeddedAttributeName = embeddedXProperty.getName();
			this.attributeConversionInfoMap = processAttributeConversions( embeddedXProperty );
		}
		else {
			this.embeddedAttributeName = "";
			this.attributeConversionInfoMap = processAttributeConversions( inferredData.getClassOrPluralElement() );
		}
	}

	/**
	 * This is called from our constructor and handles (in order):<ol>
	 *     <li>@Convert annotation at the Embeddable class level</li>
	 *     <li>@Converts annotation at the Embeddable class level</li>
	 *     <li>@Convert annotation at the Embedded attribute level</li>
	 *     <li>@Converts annotation at the Embedded attribute level</li>
	 * </ol>
	 * <p>
	 * The order is important to ensure proper precedence.
	 * <p>
	 * {@literal @Convert/@Converts} annotations at the Embeddable attribute level are handled in the calls to
	 * {@link #startingProperty}.  Duplicates are simply ignored there.
	 *
	 * @param embeddedXProperty The property that is the composite being described by this ComponentPropertyHolder
	 */
	private Map<String,AttributeConversionInfo> processAttributeConversions(XProperty embeddedXProperty) {
		final Map<String,AttributeConversionInfo> infoMap = new HashMap<>();

		final XClass embeddableXClass = embeddedXProperty.getType();

		// as a baseline, we want to apply conversions from the Embeddable and then overlay conversions
		// from the Embedded

		// first apply conversions from the Embeddable...
		processAttributeConversions( embeddableXClass, infoMap );

		// then we can overlay any conversions from the Embedded attribute
		{
			// @Convert annotation on the Embedded attribute
			final Convert convertAnnotation = embeddedXProperty.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, embeddableXClass );
				if ( isEmpty( info.getAttributeName() ) ) {
					throw new IllegalStateException( "Convert placed on Embedded attribute must define (sub)attributeName" );
				}
				infoMap.put( info.getAttributeName(), info );
			}
		}
		{
			// @Converts annotation on the Embedded attribute
			final Converts convertsAnnotation = embeddedXProperty.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, embeddableXClass );
					if ( isEmpty( info.getAttributeName() ) ) {
						throw new IllegalStateException( "Convert placed on Embedded attribute must define (sub)attributeName" );
					}
					infoMap.put( info.getAttributeName(), info );
				}
			}
		}

		return infoMap;
	}

	private void processAttributeConversions(XClass embeddableXClass, Map<String, AttributeConversionInfo> infoMap) {
		{
			// @Convert annotation on the Embeddable class level
			final Convert convertAnnotation = embeddableXClass.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, embeddableXClass );
				if ( isEmpty( info.getAttributeName() ) ) {
					throw new IllegalStateException( "@Convert placed on @Embeddable must define attributeName" );
				}
				infoMap.put( info.getAttributeName(), info );
			}
		}
		{
			// @Converts annotation on the Embeddable class level
			final Converts convertsAnnotation = embeddableXClass.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, embeddableXClass );
					if ( isEmpty( info.getAttributeName() ) ) {
						throw new IllegalStateException( "@Converts placed on @Embeddable must define attributeName" );
					}
					infoMap.put( info.getAttributeName(), info );
				}
			}
		}
	}

	private Map<String,AttributeConversionInfo> processAttributeConversions(XClass embeddableXClass) {
		final Map<String,AttributeConversionInfo> infoMap = new HashMap<>();
		processAttributeConversions( embeddableXClass, infoMap );
		return infoMap;
	}

	@Override
	protected String normalizeCompositePath(String attributeName) {
		return embeddedAttributeName + '.' + attributeName;
	}

	@Override
	protected String normalizeCompositePathForLogging(String attributeName) {
		return normalizeCompositePath( attributeName );
	}

	@Override
	public void startingProperty(XProperty property) {
		if ( property == null ) {
			return;
		}

		// again : the property coming in here *should* be the property on the embeddable (Address#city in the example),
		// so we just ignore it if there is already an existing conversion info for that path since they would have
		// precedence

		// technically we should only do this for properties of "basic type"

		final String path = embeddedAttributeName + '.' + property.getName();
		if ( attributeConversionInfoMap.containsKey( path ) ) {
			return;
		}

		{
			// @Convert annotation on the Embeddable attribute
			final Convert convertAnnotation = property.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, property );
				attributeConversionInfoMap.put( property.getName(), info );
			}
		}
		{
			// @Converts annotation on the Embeddable attribute
			final Converts convertsAnnotation = property.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, property );
					attributeConversionInfoMap.put( property.getName(), info );
				}
			}
		}
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(XProperty property) {
		// conversions on parent would have precedence
		return locateAttributeConversionInfo( property.getName() );
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(String path) {
		final String embeddedPath = qualifyConditionally( embeddedAttributeName, path );
		final AttributeConversionInfo fromParent = parent.locateAttributeConversionInfo( embeddedPath );
		if ( fromParent != null ) {
			return fromParent;
		}

		final AttributeConversionInfo fromEmbedded = attributeConversionInfoMap.get( embeddedPath );
		if ( fromEmbedded != null ) {
			return fromEmbedded;
		}

		return attributeConversionInfoMap.get( path );
	}

	@Override
	public String getEntityName() {
		return component.getComponentClassName();
	}

	@Override
	public void addProperty(Property property, AnnotatedColumns columns, XClass declaringClass) {
		//AnnotatedColumns.checkPropertyConsistency( ); //already called earlier
		// Check table matches between the component and the columns
		// if not, change the component table if no properties are set
		// if a property is set already the core cannot support that
		assert columns == null || property.getValue().getTable() == columns.getTable();
		setTable( property.getValue().getTable() );
		addProperty( property, declaringClass );
	}

	private void setTable(Table table) {
		if ( !table.equals( getTable() ) ) {
			if ( component.getPropertySpan() == 0 ) {
				component.setTable( table );
			}
			else {
				throw new AnnotationException(
						"Embeddable class '" + component.getComponentClassName()
						+ "' has properties mapped to two different tables"
						+ " (all properties of the embeddable class must map to the same table)"
				);
			}
			if ( parent instanceof ComponentPropertyHolder ) {
				( (ComponentPropertyHolder) parent ).setTable( table );
			}
		}
	}

	@Override
	public Join addJoin(JoinTable joinTable, boolean noDelayInPkColumnCreation) {
		return parent.addJoin( joinTable, noDelayInPkColumnCreation );
	}

	@Override
	public Join addJoin(JoinTable joinTable, Table table, boolean noDelayInPkColumnCreation) {
		return parent.addJoin( joinTable, table, noDelayInPkColumnCreation );
	}

	@Override
	public String getClassName() {
		return component.getComponentClassName();
	}

	@Override
	public String getEntityOwnerClassName() {
		return component.getOwner().getClassName();
	}

	public AggregateColumn getAggregateColumn() {
		final AggregateColumn aggregateColumn = component.getAggregateColumn();
		return aggregateColumn != null ? aggregateColumn : component.getParentAggregateColumn();
	}

	@Override
	public Table getTable() {
		return component.getTable();
	}

	@Override
	public void addProperty(Property prop, XClass declaringClass) {
		handleGenericComponentProperty( prop, getContext() );
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState != null && inheritanceState.isEmbeddableSuperclass() ) {
				addPropertyToMappedSuperclass( prop, declaringClass, getContext() );
			}
		}
		component.addProperty( prop, declaringClass );
	}

	@Override
	public void movePropertyToJoin(Property prop, Join join, XClass declaringClass) {
		// or maybe only throw if component.getTable() != join.getTable()
		throw new AnnotationException(
				"Embeddable class '" + component.getComponentClassName()
				+ "' has an unowned @OneToOne property " + prop.getName()
				+ "mapped to a join table which is unsupported"
		);
	}

	@Override
	public KeyValue getIdentifier() {
		return component.getOwner().getIdentifier();
	}

	@Override
	public boolean isOrWithinEmbeddedId() {
		return isOrWithinEmbeddedId;
	}

	@Override
	public boolean isWithinElementCollection() {
		return isWithinElementCollection;
	}

	@Override
	public PersistentClass getPersistentClass() {
		return component.getOwner();
	}

	@Override
	public boolean isComponent() {
		return true;
	}

	@Override
	public boolean isEntity() {
		return false;
	}

	@Override
	public void setParentProperty(String parentProperty) {
		component.setParentProperty( parentProperty );
	}

	@Override
	public Column[] getOverriddenColumn(String propertyName) {
		//FIXME this is yukky
		Column[] result = super.getOverriddenColumn( propertyName );
		if ( result == null ) {
			String userPropertyName = extractUserPropertyName( "id", propertyName );
			if ( userPropertyName != null ) {
				result = super.getOverriddenColumn( userPropertyName );
			}
		}
		if ( result == null ) {
			String userPropertyName = extractUserPropertyName( IDENTIFIER_MAPPER_PROPERTY, propertyName );
			if ( userPropertyName != null ) {
				result = super.getOverriddenColumn( userPropertyName );
			}
		}
		return result;
	}

	private String extractUserPropertyName(String redundantString, String propertyName) {
		String className = component.getOwner().getClassName();
		boolean specialCase = propertyName.startsWith(className)
				&& propertyName.length() > className.length() + 2 + redundantString.length() // .id.
				&& propertyName.substring( className.length() + 1, className.length() + 1 + redundantString.length() )
						.equals(redundantString);
		if (specialCase) {
			//remove id we might be in a @IdClass case
			return className + propertyName.substring( className.length() + 1 + redundantString.length() );
		}
		return null;
	}

	@Override
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		return super.getOverriddenJoinColumn( propertyName );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + parent.normalizeCompositePathForLogging( embeddedAttributeName ) + ")";
	}
}
