/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.JoinTable;

import static org.hibernate.boot.model.internal.ClassPropertyHolder.addPropertyToMappedSuperclass;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.model.internal.PropertyBinder.hasIdAnnotation;
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
	private final Map<ClassDetails, InheritanceState> inheritanceStatePerClass;

	private final String embeddedAttributeName;
	private final Map<String,AttributeConversionInfo> attributeConversionInfoMap;

	public ComponentPropertyHolder(
			Component component,
			String path,
			PropertyData inferredData,
			PropertyHolder parent,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		super( path, parent, inferredData.getPropertyType().determineRawClass(), context );
		final MemberDetails embeddedMemberDetails = inferredData.getAttributeMember();
		setCurrentProperty( embeddedMemberDetails );
		this.component = component;
		this.isOrWithinEmbeddedId = parent.isOrWithinEmbeddedId()
				|| embeddedMemberDetails != null && hasIdAnnotation( embeddedMemberDetails );
		this.isWithinElementCollection = parent.isWithinElementCollection()
				|| parent instanceof CollectionPropertyHolder;
		this.inheritanceStatePerClass = inheritanceStatePerClass;

		if ( embeddedMemberDetails != null ) {
			this.embeddedAttributeName = embeddedMemberDetails.getName();
			this.attributeConversionInfoMap = processAttributeConversions( embeddedMemberDetails );
		}
		else {
			this.embeddedAttributeName = "";
			this.attributeConversionInfoMap = processAttributeConversions( inferredData.getClassOrElementType() );
		}
	}

	/**
	 * Access to the underlying component
	 */
	public Component getComponent() {
		return component;
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
	 * @param embeddedMemberDetails The property that is the composite being described by this ComponentPropertyHolder
	 */
	private Map<String,AttributeConversionInfo> processAttributeConversions(MemberDetails embeddedMemberDetails) {
		final Map<String,AttributeConversionInfo> infoMap = new HashMap<>();

		final TypeDetails embeddableTypeDetails = embeddedMemberDetails.getType();

		// as a baseline, we want to apply conversions from the Embeddable and then overlay conversions
		// from the Embedded

		// first apply conversions from the Embeddable...
		processAttributeConversions( embeddableTypeDetails, infoMap );

		// then we can overlay any conversions from the Embedded attribute
		embeddedMemberDetails.forEachAnnotationUsage( Convert.class, getSourceModelContext(), (usage) -> {
			final AttributeConversionInfo info = new AttributeConversionInfo( usage, embeddedMemberDetails );
			if ( isEmpty( info.getAttributeName() ) ) {
				throw new IllegalStateException( "Convert placed on Embedded attribute must define (sub)attributeName" );
			}
			infoMap.put( info.getAttributeName(), info );
		} );

		return infoMap;
	}

	private void processAttributeConversions(TypeDetails embeddableTypeDetails, Map<String, AttributeConversionInfo> infoMap) {
		final ClassDetails embeddableClassDetails = embeddableTypeDetails.determineRawClass();
		embeddableClassDetails.forEachAnnotationUsage( Convert.class, getSourceModelContext(), (usage) -> {
			final AttributeConversionInfo info = new AttributeConversionInfo( usage, embeddableClassDetails );
			if ( isEmpty( info.getAttributeName() ) ) {
				throw new IllegalStateException( "@Convert placed on @Embeddable must define attributeName" );
			}
			infoMap.put( info.getAttributeName(), info );
		} );
	}

	private Map<String,AttributeConversionInfo> processAttributeConversions(TypeDetails embeddableTypeDetails) {
		final Map<String,AttributeConversionInfo> infoMap = new HashMap<>();
		processAttributeConversions( embeddableTypeDetails, infoMap );
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
	public void startingProperty(MemberDetails propertyMemberDetails) {
		if ( propertyMemberDetails == null ) {
			return;
		}

		// again : the property coming in here *should* be the property on the embeddable (Address#city in the example),
		// so we just ignore it if there is already an existing conversion info for that path since they would have
		// precedence

		// technically we should only do this for properties of "basic type"

		final String attributeName = propertyMemberDetails.resolveAttributeName();
		final String path = embeddedAttributeName + '.' + attributeName;
		if ( attributeConversionInfoMap.containsKey( path ) ) {
			return;
		}

		propertyMemberDetails.forEachAnnotationUsage( Convert.class, getSourceModelContext(), (usage) -> {
			final AttributeConversionInfo info = new AttributeConversionInfo( usage, propertyMemberDetails );
			attributeConversionInfoMap.put( attributeName, info );
		} );
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(MemberDetails attributeMember) {
		// conversions on parent would have precedence
		return locateAttributeConversionInfo( attributeMember.resolveAttributeName() );
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
	public void addProperty(Property property, MemberDetails attributeMemberDetails, AnnotatedColumns columns, ClassDetails declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		// Check table matches between the component and the columns
		// if not, change the component table if no properties are set
		// if a property is set already the core cannot support that
		if ( columns != null ) {
			final Table table = columns.getTable();
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
			}
		}
		addProperty( property, attributeMemberDetails, declaringClass );
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
	public void addProperty(Property prop, MemberDetails attributeMemberDetails, ClassDetails declaringClass) {
		handleGenericComponentProperty( prop, attributeMemberDetails, getContext() );
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState != null && inheritanceState.isEmbeddableSuperclass() ) {
				addPropertyToMappedSuperclass( prop, attributeMemberDetails, declaringClass, getContext() );
			}
		}
		component.addProperty( prop, declaringClass );
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
			final String userPropertyName = extractUserPropertyName( "id", propertyName );
			if ( userPropertyName != null ) {
				result = super.getOverriddenColumn( userPropertyName );
			}
		}
		if ( result == null ) {
			final String userPropertyName = extractUserPropertyName( IDENTIFIER_MAPPER_PROPERTY, propertyName );
			if ( userPropertyName != null ) {
				result = super.getOverriddenColumn( userPropertyName );
			}
		}
		return result;
	}

	private String extractUserPropertyName(String redundantString, String propertyName) {
		String className = component.getOwner().getClassName();
		if ( className != null && propertyName.startsWith( className ) ) {
			boolean specialCase = propertyName.length() > className.length() + 2 + redundantString.length()
					&& propertyName.substring( className.length() + 1, className.length() + 1 + redundantString.length() ).equals( redundantString );
			if ( specialCase ) {
				//remove id we might be in a @IdClass case
				return className + propertyName.substring( className.length() + 1 + redundantString.length() );
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + parent.normalizeCompositePathForLogging( embeddedAttributeName ) + ")";
	}
}
