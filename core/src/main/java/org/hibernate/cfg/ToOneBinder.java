package org.hibernate.cfg;

import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

/**
 * Work in progress
 * The goal of this class is to aggregate all operations
 * related to ToOne binding operations
 *
 * @author Emmanuel Bernard
 */
public class ToOneBinder {
	public static String getReferenceEntityName(PropertyData propertyData, XClass targetEntity, ExtendedMappings mappings) {
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}

	public static String getReferenceEntityName(PropertyData propertyData, ExtendedMappings mappings) {
		XClass targetEntity = getTargetEntity( propertyData, mappings );
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}

	public static XClass getTargetEntity(PropertyData propertyData, ExtendedMappings mappings) {
		XProperty property = propertyData.getProperty();
		return mappings.getReflectionManager().toXClass( getTargetEntityClass( property ) );
	}

	private static Class<?> getTargetEntityClass(XProperty property) {
		final ManyToOne mTo = property.getAnnotation( ManyToOne.class );
		if (mTo != null) {
			return mTo.targetEntity();
		}
		final OneToOne oTo = property.getAnnotation( OneToOne.class );
		if (oTo != null) {
			return oTo.targetEntity();
		}
		throw new AssertionFailure("Unexpected discovery of a targetEntity: " + property.getName() );
	}
}
