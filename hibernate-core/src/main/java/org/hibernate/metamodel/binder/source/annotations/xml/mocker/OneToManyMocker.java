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
package org.hibernate.metamodel.binder.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLOneToMany;

/**
 * @author Strong Liu
 */
class OneToManyMocker extends PropertyMocker {
	private XMLOneToMany oneToMany;

	OneToManyMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, XMLOneToMany oneToMany) {
		super( indexBuilder, classInfo, defaults );
		this.oneToMany = oneToMany;
	}

	@Override
	protected String getFieldName() {
		return oneToMany.getName();
	}

	@Override
	protected void processExtra() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue(
				"targetEntity", oneToMany.getTargetEntity(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		MockHelper.enumValue( "fetch", FETCH_TYPE, oneToMany.getFetch(), annotationValueList );
		MockHelper.stringValue( "mappedBy", oneToMany.getMappedBy(), annotationValueList );
		MockHelper.booleanValue( "orphanRemoval", oneToMany.isOrphanRemoval(), annotationValueList );
		MockHelper.cascadeValue( "cascade", oneToMany.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( ONE_TO_MANY, getTarget(), annotationValueList );
		parserAttributeOverrides( oneToMany.getMapKeyAttributeOverride(), getTarget() );
		parserMapKeyJoinColumnList( oneToMany.getMapKeyJoinColumn(), getTarget() );
		parserMapKey( oneToMany.getMapKey(), getTarget() );
		parserMapKeyColumn( oneToMany.getMapKeyColumn(), getTarget() );
		parserMapKeyClass( oneToMany.getMapKeyClass(), getTarget() );
		parserMapKeyTemporal( oneToMany.getMapKeyTemporal(), getTarget() );
		parserMapKeyEnumerated( oneToMany.getMapKeyEnumerated(), getTarget() );
		parserJoinColumnList( oneToMany.getJoinColumn(), getTarget() );
		parserOrderColumn( oneToMany.getOrderColumn(), getTarget() );
		parserJoinTable( oneToMany.getJoinTable(), getTarget() );
		if ( oneToMany.getOrderBy() != null ) {
			create( ORDER_BY, getTarget(), MockHelper.stringValueArray( "value", oneToMany.getOrderBy() ) );
		}
	}

	@Override
	protected XMLAccessType getAccessType() {
		return oneToMany.getAccess();
	}

	@Override
	protected void setAccessType(XMLAccessType accessType) {
		oneToMany.setAccess( accessType );
	}
}
