/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.PropertyPath;
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

/**
 * PropertyHolder for composites (Embeddable/Embedded).
 * <p/>
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
 *
 * As you can see, lots of ways to specify the conversion for embeddable attributes :(
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class ComponentPropertyHolder extends AbstractPropertyHolder {
	//TODO introduce an overrideTable() method for columns held by sec table rather than the hack
	//     joinsPerRealTableName in ClassPropertyHolder
	private final Component component;
	private final boolean isOrWithinEmbeddedId;
	private final boolean isWithinElementCollection;

//	private boolean virtual;
	private final String embeddedAttributeName;
	private final Map<String,AttributeConversionInfo> attributeConversionInfoMap;

	public ComponentPropertyHolder(
			Component component,
			String path,
			PropertyData inferredData,
			PropertyHolder parent,
			MetadataBuildingContext context) {
		super( path, parent, inferredData.getPropertyClass(), context );
		final XProperty embeddedXProperty = inferredData.getProperty();
		setCurrentProperty( embeddedXProperty );
		this.component = component;
		this.isOrWithinEmbeddedId = parent.isOrWithinEmbeddedId()
				|| ( embeddedXProperty != null && hasIdAnnotation( embeddedXProperty ) );
		this.isWithinElementCollection = parent.isWithinElementCollection()
				|| parent instanceof CollectionPropertyHolder;

		// NOTE : in terms of conversions here, we have 2 cases:
		//
		// 1. The converter is for the embedded value itself
		// 2. One or more converters apply to sub-attributes


		if ( embeddedXProperty != null ) {
//			this.virtual = false;
			this.embeddedAttributeName = embeddedXProperty.getName();
			this.attributeConversionInfoMap = processAttributeConversions( embeddedXProperty );
		}
		else {
			// could be either:
			// 		1) virtual/dynamic component
			// 		2) collection element/key

			// temp
//			this.virtual = true;
			this.embeddedAttributeName = "";
			this.attributeConversionInfoMap = processAttributeConversions( inferredData.getClassOrElement() );
		}
	}

	private boolean hasIdAnnotation(XProperty embeddedXProperty) {
		return embeddedXProperty.isAnnotationPresent( Id.class )
				|| embeddedXProperty.isAnnotationPresent( EmbeddedId.class );
	}

	/**
	 * Find the converter, if one, intended for the composition as a whole rather
	 * than its sub-attributes.
	 *
	 * @return The converter, or null if there is not
	 */
	private AttributeConversionInfo determineEmbeddedConverter(XProperty embeddedXProperty, MetadataBuildingContext context) {
		final Convert convertAnnotation = embeddedXProperty.getAnnotation( Convert.class );
		if ( convertAnnotation != null ) {
			// if the convert annotation defines a sub-attribute name, then it does not apply to the composition itself
			if ( StringHelper.isEmpty( convertAnnotation.attributeName() ) ) {
				return null;
			}
			else if ( convertAnnotation.disableConversion() ) {
				return null;
			}
			else {
				// otherwise, apply the conversion at the composition-level
				return new AttributeConversionInfo( convertAnnotation, embeddedXProperty.getType() );
			}
		}

		// check to see if it defines multiple, which indicates there is no compositional conversion
		final Converts convertsAnnotation = embeddedXProperty.getAnnotation( Converts.class );
		if ( convertsAnnotation != null && convertsAnnotation.value().length > 0 ) {
			return null;
		}

		// see if there is an auto-apply conversion registered...
		final ConverterDescriptor autoAppliedConverter = context
				.getMetadataCollector()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForAttribute( embeddedXProperty, context );
		if ( autoAppliedConverter == null ) {
			return null;
		}

		return new AttributeConversionInfo(
				autoAppliedConverter.getAttributeConverterClass(),
				false,
				embeddedAttributeName,
				embeddedXProperty
		);

	}

	/**
	 * This is called from our constructor and handles (in order):<ol>
	 *     <li>@Convert annotation at the Embeddable class level</li>
	 *     <li>@Converts annotation at the Embeddable class level</li>
	 *     <li>@Convert annotation at the Embedded attribute level</li>
	 *     <li>@Converts annotation at the Embedded attribute level</li>
	 * </ol>
	 * <p/>
	 * The order is important to ensure proper precedence.
	 * <p/>
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
				if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
					//noinspection StatementWithEmptyBody
					if ( getContext().getBootstrapContext().getJpaCompliance().isConverterComplianceEnabled() ) {
						throw new IllegalStateException( "Convert placed on Embedded attribute must define (sub) attribute-name" );
					}
					else {
						// assume it targets the embedded value itself
					}
				}
				else {
					infoMap.put( info.getAttributeName(), info );
				}
			}
		}
		{
			// @Converts annotation on the Embedded attribute
			final Converts convertsAnnotation = embeddedXProperty.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, embeddableXClass );
					if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
						throw new IllegalStateException( "Convert placed on Embedded attribute must define (sub) attribute-name" );
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

				if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
					//noinspection StatementWithEmptyBody
					if ( getContext().getBootstrapContext().getJpaCompliance().isConverterComplianceEnabled() ) {
						throw new IllegalStateException( "Convert placed on Embedded attribute must define (sub) attribute-name" );
					}
					else {
						// assume it targets the embedded value itself
					}
				}
				else {
					infoMap.put( info.getAttributeName(), info );
				}
			}
		}

		{
			// @Converts annotation on the Embeddable class level
			final Converts convertsAnnotation = embeddableXClass.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, embeddableXClass );
					if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
						throw new IllegalStateException( "@Converts placed on @Embeddable must define (sub) attribute-name" );
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

//		if ( virtual ) {
//			return;
//		}

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
		final String embeddedPath = StringHelper.qualifyConditionally( embeddedAttributeName, path );
		AttributeConversionInfo fromParent = parent.locateAttributeConversionInfo( embeddedPath );
		if ( fromParent != null ) {
			return fromParent;
		}

		AttributeConversionInfo fromEmbedded = attributeConversionInfoMap.get( embeddedPath );
		if ( fromEmbedded != null ) {
			return fromEmbedded;
		}

		return attributeConversionInfoMap.get( path );
	}

	public String getEntityName() {
		return component.getComponentClassName();
	}

	public void addProperty(Property prop, AnnotatedColumn[] columns, XClass declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		/*
		 * Check table matches between the component and the columns
		 * if not, change the component table if no properties are set
		 * if a property is set already the core cannot support that
		 */
		if (columns != null) {
			Table table = columns[0].getTable();
			if ( !table.equals( component.getTable() ) ) {
				if ( component.getPropertySpan() == 0 ) {
					component.setTable( table );
				}
				else {
					throw new AnnotationException(
							"A component cannot hold properties split into 2 different tables: "
									+ this.getPath()
					);
				}
			}
		}
		addProperty( prop, declaringClass );
	}

	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		return parent.addJoin( joinTableAnn, noDelayInPkColumnCreation );

	}

	public String getClassName() {
		return component.getComponentClassName();
	}

	public String getEntityOwnerClassName() {
		return component.getOwner().getClassName();
	}

	public Table getTable() {
		return component.getTable();
	}

	public void addProperty(Property prop, XClass declaringClass) {
		component.addProperty( prop );
	}

	public KeyValue getIdentifier() {
		return component.getOwner().getIdentifier();
	}

	public boolean isOrWithinEmbeddedId() {
		return isOrWithinEmbeddedId;
	}

	public boolean isWithinElementCollection() {
		return isWithinElementCollection;
	}

	public PersistentClass getPersistentClass() {
		return component.getOwner();
	}

	public boolean isComponent() {
		return true;
	}

	public boolean isEntity() {
		return false;
	}

	public void setParentProperty(String parentProperty) {
		component.setParentProperty( parentProperty );
	}

	@Override
	public Column[] getOverriddenColumn(String propertyName) {
		//FIXME this is yukky
		Column[] result = super.getOverriddenColumn( propertyName );
		if ( result == null ) {
			String userPropertyName = extractUserPropertyName( "id", propertyName );
			if ( userPropertyName != null ) result = super.getOverriddenColumn( userPropertyName );
		}
		if ( result == null ) {
			String userPropertyName = extractUserPropertyName( PropertyPath.IDENTIFIER_MAPPER_PROPERTY, propertyName );
			if ( userPropertyName != null ) result = super.getOverriddenColumn( userPropertyName );
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
		return super.toString() + "(" + parent.normalizeCompositePathForLogging( embeddedAttributeName ) + ")";
	}
}
