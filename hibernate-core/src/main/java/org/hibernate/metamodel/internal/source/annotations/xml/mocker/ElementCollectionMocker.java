/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import org.hibernate.jaxb.spi.orm.JaxbElementCollection;

/**
 * @author Strong Liu
 */
class ElementCollectionMocker extends PropertyMocker {
	private final JaxbElementCollection elementCollection;

	ElementCollectionMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbElementCollection elementCollection) {
		super( indexBuilder, classInfo, defaults );
		this.elementCollection = elementCollection;
	}

	@Override
	protected PropertyElement getPropertyElement() {
		return elementCollection;
	}

	@Override
	protected void processExtra() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue(
				"targetClass",
				elementCollection.getTargetClass(),
				annotationValueList,
				indexBuilder.getServiceRegistry()
		);
		MockHelper.enumValue( "fetch", FETCH_TYPE, elementCollection.getFetch(), annotationValueList );
		create( ELEMENT_COLLECTION, annotationValueList );
		parseLob( elementCollection.getLob(), getTarget() );
		parseEnumType( elementCollection.getEnumerated(), getTarget() );
		parseColumn( elementCollection.getColumn(), getTarget() );
		parseTemporalType( elementCollection.getTemporal(), getTarget() );
		parseCollectionTable( elementCollection.getCollectionTable(), getTarget() );
		parseAssociationOverrides( elementCollection.getAssociationOverride(), getTarget() );
		parseAttributeOverrides( elementCollection.getAttributeOverride(), getTarget() );
		if ( elementCollection.getOrderBy() != null ) {
			create( ORDER_BY, MockHelper.stringValueArray( "value", elementCollection.getOrderBy() ) );
		}
		parseAttributeOverrides( elementCollection.getMapKeyAttributeOverride(), getTarget() );
		parseMapKeyJoinColumnList( elementCollection.getMapKeyJoinColumn(), getTarget() );
		parseMapKey( elementCollection.getMapKey(), getTarget() );
		parseMapKeyColumn( elementCollection.getMapKeyColumn(), getTarget() );
		parseMapKeyClass( elementCollection.getMapKeyClass(), getTarget() );
		parseMapKeyEnumerated( elementCollection.getMapKeyEnumerated(), getTarget() );
		parseMapKeyTemporal( elementCollection.getMapKeyTemporal(), getTarget() );
	}
}
