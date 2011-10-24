/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
	public static String getReferenceEntityName(PropertyData propertyData, XClass targetEntity, Mappings mappings) {
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}

	public static String getReferenceEntityName(PropertyData propertyData, Mappings mappings) {
		XClass targetEntity = getTargetEntity( propertyData, mappings );
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}

	public static XClass getTargetEntity(PropertyData propertyData, Mappings mappings) {
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
