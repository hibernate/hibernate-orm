/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Work in progress
 * The goal of this class is to aggregate all operations
 * related to ToOne binding operations
 *
 * @author Emmanuel Bernard
 */
public class ToOneBinder {
	public static String getReferenceEntityName(PropertyData propertyData, XClass targetEntity, MetadataBuildingContext buildingContext) {
		if ( AnnotationBinder.isDefault( targetEntity, buildingContext ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}

	public static String getReferenceEntityName(PropertyData propertyData, MetadataBuildingContext buildingContext) {
		XClass targetEntity = getTargetEntity( propertyData, buildingContext );
		if ( AnnotationBinder.isDefault( targetEntity, buildingContext ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}

	public static XClass getTargetEntity(PropertyData propertyData, MetadataBuildingContext buildingContext) {
		XProperty property = propertyData.getProperty();
		return buildingContext.getBootstrapContext().getReflectionManager().toXClass( getTargetEntityClass( property ) );
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
