/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Convert;
import javax.persistence.Converts;
import javax.persistence.JoinTable;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard

 */
public class ClassPropertyHolder extends AbstractPropertyHolder {
	private PersistentClass persistentClass;
	private Map<String, Join> joins;
	private transient Map<String, Join> joinsPerRealTableName;
	private EntityBinder entityBinder;
	private final Map<XClass, InheritanceState> inheritanceStatePerClass;

	private Map<String,AttributeConversionInfo> attributeConversionInfoMap;

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass entityXClass,
			Map<String, Join> joins,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		super( persistentClass.getEntityName(), null, entityXClass, context );
		this.persistentClass = persistentClass;
		this.joins = joins;
		this.inheritanceStatePerClass = inheritanceStatePerClass;

		this.attributeConversionInfoMap = buildAttributeConversionInfoMap( entityXClass );
	}

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass entityXClass,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this( persistentClass, entityXClass, entityBinder.getSecondaryTables(), context, inheritanceStatePerClass );
		this.entityBinder = entityBinder;
	}

	@Override
	protected String normalizeCompositePath(String attributeName) {
		return attributeName;
	}

	@Override
	protected String normalizeCompositePathForLogging(String attributeName) {
		return getEntityName() + '.' + attributeName;
	}

	protected Map<String, AttributeConversionInfo> buildAttributeConversionInfoMap(XClass entityXClass) {
		final HashMap<String, AttributeConversionInfo> map = new HashMap<String, AttributeConversionInfo>();
		collectAttributeConversionInfo( map, entityXClass );
		return map;
	}

	private void collectAttributeConversionInfo(Map<String, AttributeConversionInfo> infoMap, XClass xClass) {
		if ( xClass == null ) {
			// typically indicates we have reached the end of the inheritance hierarchy
			return;
		}

		// collect superclass info first
		collectAttributeConversionInfo( infoMap, xClass.getSuperclass() );

		final boolean canContainConvert = xClass.isAnnotationPresent( javax.persistence.Entity.class )
				|| xClass.isAnnotationPresent( javax.persistence.MappedSuperclass.class )
				|| xClass.isAnnotationPresent( javax.persistence.Embeddable.class );
		if ( ! canContainConvert ) {
			return;
		}

		{
			final Convert convertAnnotation = xClass.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, xClass );
				if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
					throw new IllegalStateException( "@Convert placed on @Entity/@MappedSuperclass must define attributeName" );
				}
				infoMap.put( info.getAttributeName(), info );
			}
		}
		{
			final Converts convertsAnnotation = xClass.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, xClass );
					if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
						throw new IllegalStateException( "@Converts placed on @Entity/@MappedSuperclass must define attributeName" );
					}
					infoMap.put( info.getAttributeName(), info );
				}
			}
		}
	}

	@Override
	public void startingProperty(XProperty property) {
		if ( property == null ) {
			return;
		}

		final String propertyName = property.getName();
		if ( attributeConversionInfoMap.containsKey( propertyName ) ) {
			return;
		}

		{
			// @Convert annotation on the Embeddable attribute
			final Convert convertAnnotation = property.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, property );
				if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
					attributeConversionInfoMap.put( propertyName, info );
				}
				else {
					attributeConversionInfoMap.put( propertyName + '.' + info.getAttributeName(), info );
				}
			}
		}
		{
			// @Converts annotation on the Embeddable attribute
			final Converts convertsAnnotation = property.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, property );
					if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
						attributeConversionInfoMap.put( propertyName, info );
					}
					else {
						attributeConversionInfoMap.put( propertyName + '.' + info.getAttributeName(), info );
					}
				}
			}
		}
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(XProperty property) {
		return locateAttributeConversionInfo( property.getName() );
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(String path) {
		return attributeConversionInfoMap.get( path );
	}

	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	public void addProperty(Property prop, Ejb3Column[] columns, XClass declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		if ( columns != null && columns[0].isSecondary() ) {
			//TODO move the getJoin() code here?
			final Join join = columns[0].getJoin();
			addPropertyToJoin( prop, declaringClass, join );
		}
		else {
			addProperty( prop, declaringClass );
		}
	}

	public void addProperty(Property prop, XClass declaringClass) {
		if ( prop.getValue() instanceof Component ) {
			//TODO handle quote and non quote table comparison
			String tableName = prop.getValue().getTable().getName();
			if ( getJoinsPerRealTableName().containsKey( tableName ) ) {
				final Join join = getJoinsPerRealTableName().get( tableName );
				addPropertyToJoin( prop, declaringClass, join );
			}
			else {
				addPropertyToPersistentClass( prop, declaringClass );
			}
		}
		else {
			addPropertyToPersistentClass( prop, declaringClass );
		}
	}

	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		Join join = entityBinder.addJoin( joinTableAnn, this, noDelayInPkColumnCreation );
		this.joins = entityBinder.getSecondaryTables();
		return join;
	}

	private void addPropertyToPersistentClass(Property prop, XClass declaringClass) {
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				persistentClass.addMappedsuperclassProperty(prop);
				addPropertyToMappedSuperclass( prop, declaringClass );
			}
			else {
				persistentClass.addProperty( prop );
			}
		}
		else {
			persistentClass.addProperty( prop );
		}
	}

	private void addPropertyToMappedSuperclass(Property prop, XClass declaringClass) {
		final Class type = getContext().getBootstrapContext().getReflectionManager().toClass( declaringClass );
		MappedSuperclass superclass = getContext().getMetadataCollector().getMappedSuperclass( type );
		superclass.addDeclaredProperty( prop );
	}

	private void addPropertyToJoin(Property prop, XClass declaringClass, Join join) {
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				join.addMappedsuperclassProperty(prop);
				addPropertyToMappedSuperclass( prop, declaringClass );
			}
			else {
				join.addProperty( prop );
			}
		}
		else {
			join.addProperty( prop );
		}
	}

	/**
	 * Needed for proper compliance with naming strategy, the property table
	 * can be overridden if the properties are part of secondary tables
	 */
	private Map<String, Join> getJoinsPerRealTableName() {
		if ( joinsPerRealTableName == null ) {
			joinsPerRealTableName = new HashMap<String, Join>( joins.size() );
			for (Join join : joins.values()) {
				joinsPerRealTableName.put( join.getTable().getName(), join );
			}
		}
		return joinsPerRealTableName;
	}

	public String getClassName() {
		return persistentClass.getClassName();
	}

	public String getEntityOwnerClassName() {
		return getClassName();
	}

	public Table getTable() {
		return persistentClass.getTable();
	}

	public boolean isComponent() {
		return false;
	}

	public boolean isEntity() {
		return true;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public KeyValue getIdentifier() {
		return persistentClass.getIdentifier();
	}

	public boolean isOrWithinEmbeddedId() {
		return false;
	}

	@Override
	public boolean isWithinElementCollection() {
		return false;
	}

	//	@Override
//	public AttributeConverterDefinition resolveAttributeConverter(String attributeName) {
//
//		// @Convert annotations take precedence if present
//		final Convert convertAnnotation = locateConvertAnnotation( property );
//		if ( convertAnnotation != null ) {
//			log.debugf(
//					"Applying located @Convert AttributeConverter [%s] to attribute [%]",
//					convertAnnotation.converter().getName(),
//					property.getName()
//			);
//			attributeConverterDefinition = getMetadata().locateAttributeConverter( convertAnnotation.converter() );
//		}
//		else {
//			attributeConverterDefinition = locateAutoApplyAttributeConverter( property );
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	private Convert locateConvertAnnotation(XProperty property) {
//		LOG.debugf(
//				"Attempting to locate Convert annotation for property [%s:%s]",
//				persistentClassName,
//				property.getName()
//		);
//
//		// first look locally on the property for @Convert/@Converts
//		{
//			Convert localConvertAnnotation = property.getAnnotation( Convert.class );
//			if ( localConvertAnnotation != null ) {
//				LOG.debugf(
//						"Found matching local @Convert annotation [disableConversion=%s]",
//						localConvertAnnotation.disableConversion()
//				);
//				return localConvertAnnotation.disableConversion()
//						? null
//						: localConvertAnnotation;
//			}
//		}
//
//		{
//			Converts localConvertsAnnotation = property.getAnnotation( Converts.class );
//			if ( localConvertsAnnotation != null ) {
//				for ( Convert localConvertAnnotation : localConvertsAnnotation.value() ) {
//					if ( isLocalMatch( localConvertAnnotation, property ) ) {
//						LOG.debugf(
//								"Found matching @Convert annotation as part local @Converts [disableConversion=%s]",
//								localConvertAnnotation.disableConversion()
//						);
//						return localConvertAnnotation.disableConversion()
//								? null
//								: localConvertAnnotation;
//					}
//				}
//			}
//		}
//
//		if ( persistentClassName == null ) {
//			LOG.debug( "Persistent Class name not known during attempt to locate @Convert annotations" );
//			return null;
//		}
//
//		final XClass owner;
//		try {
//			final Class ownerClass = ReflectHelper.classForName( persistentClassName );
//			owner = mappings.getReflectionManager().classForName( persistentClassName, ownerClass  );
//		}
//		catch (ClassNotFoundException e) {
//			throw new AnnotationException( "Unable to resolve Class reference during attempt to locate @Convert annotations" );
//		}
//
//		return lookForEntityDefinedConvertAnnotation( property, owner );
//	}


	@Override
	public String toString() {
		return super.toString() + "(" + getEntityName() + ")";
	}
}
