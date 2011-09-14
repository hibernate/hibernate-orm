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
import org.hibernate.internal.jaxb.mapping.orm.JaxbManyToMany;

/**
 * @author Strong Liu
 */
class ManyToManyMocker extends PropertyMocker {
	private JaxbManyToMany manyToMany;

	ManyToManyMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbManyToMany manyToMany) {
		super( indexBuilder, classInfo, defaults );
		this.manyToMany = manyToMany;
	}

	@Override
	protected String getFieldName() {
		return manyToMany.getName();
	}


	@Override
	protected void processExtra() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue(
				"targetEntity", manyToMany.getTargetEntity(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		MockHelper.enumValue( "fetch", FETCH_TYPE, manyToMany.getFetch(), annotationValueList );
		MockHelper.stringValue( "mappedBy", manyToMany.getMappedBy(), annotationValueList );
		MockHelper.cascadeValue( "cascade", manyToMany.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( MANY_TO_MANY, annotationValueList );
		parserMapKeyClass( manyToMany.getMapKeyClass(), getTarget() );
		parserMapKeyTemporal( manyToMany.getMapKeyTemporal(), getTarget() );
		parserMapKeyEnumerated( manyToMany.getMapKeyEnumerated(), getTarget() );
		parserMapKey( manyToMany.getMapKey(), getTarget() );
		parserAttributeOverrides( manyToMany.getMapKeyAttributeOverride(), getTarget() );
		parserMapKeyJoinColumnList( manyToMany.getMapKeyJoinColumn(), getTarget() );
		parserOrderColumn( manyToMany.getOrderColumn(), getTarget() );
		parserJoinTable( manyToMany.getJoinTable(), getTarget() );
		if ( manyToMany.getOrderBy() != null ) {
			create( ORDER_BY, MockHelper.stringValueArray( "value", manyToMany.getOrderBy() ) );
		}
	}

	@Override
	protected JaxbAccessType getAccessType() {
		return manyToMany.getAccess();
	}

	@Override
	protected void setAccessType(JaxbAccessType accessType) {
		manyToMany.setAccess( accessType );
	}
}
