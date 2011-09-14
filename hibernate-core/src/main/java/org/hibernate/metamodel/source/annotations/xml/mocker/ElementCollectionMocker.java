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
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbElementCollection;

/**
 * @author Strong Liu
 */
class ElementCollectionMocker extends PropertyMocker {
	private JaxbElementCollection elementCollection;

	ElementCollectionMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbElementCollection elementCollection) {
		super( indexBuilder, classInfo, defaults );
		this.elementCollection = elementCollection;
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
		parserLob( elementCollection.getLob(), getTarget() );
		parserEnumType( elementCollection.getEnumerated(), getTarget() );
		parserColumn( elementCollection.getColumn(), getTarget() );
		parserTemporalType( elementCollection.getTemporal(), getTarget() );
		parserCollectionTable( elementCollection.getCollectionTable(), getTarget() );
		parserAssociationOverrides( elementCollection.getAssociationOverride(), getTarget() );
		parserAttributeOverrides( elementCollection.getAttributeOverride(), getTarget() );
		if ( elementCollection.getOrderBy() != null ) {
			create( ORDER_BY, MockHelper.stringValueArray( "value", elementCollection.getOrderBy() ) );
		}
		parserAttributeOverrides( elementCollection.getMapKeyAttributeOverride(), getTarget() );
		parserMapKeyJoinColumnList( elementCollection.getMapKeyJoinColumn(), getTarget() );
		parserMapKey( elementCollection.getMapKey(), getTarget() );
		parserMapKeyColumn( elementCollection.getMapKeyColumn(), getTarget() );
		parserMapKeyClass( elementCollection.getMapKeyClass(), getTarget() );
		parserMapKeyEnumerated( elementCollection.getMapKeyEnumerated(), getTarget() );
		parserMapKeyTemporal( elementCollection.getMapKeyTemporal(), getTarget() );
	}

	@Override
	protected String getFieldName() {
		return elementCollection.getName();
	}

	@Override
	protected JaxbAccessType getAccessType() {
		return elementCollection.getAccess();
	}

	@Override
	protected void setAccessType(JaxbAccessType accessType) {
		elementCollection.setAccess( accessType );
	}
}
