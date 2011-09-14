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
import org.hibernate.internal.jaxb.mapping.orm.JaxbManyToOne;

/**
 * @author Strong Liu
 */
class ManyToOneMocker extends PropertyMocker {
	private JaxbManyToOne manyToOne;

	ManyToOneMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbManyToOne manyToOne) {
		super( indexBuilder, classInfo, defaults );
		this.manyToOne = manyToOne;
	}

	@Override
	protected String getFieldName() {
		return manyToOne.getName();
	}

	@Override
	protected void processExtra() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue(
				"targetEntity", manyToOne.getTargetEntity(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		MockHelper.enumValue( "fetch", FETCH_TYPE, manyToOne.getFetch(), annotationValueList );
		MockHelper.booleanValue( "optional", manyToOne.isOptional(), annotationValueList );
		MockHelper.cascadeValue( "cascade", manyToOne.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( MANY_TO_ONE, annotationValueList );
		parserJoinColumnList( manyToOne.getJoinColumn(), getTarget() );
		parserJoinTable( manyToOne.getJoinTable(), getTarget() );
		if ( manyToOne.getMapsId() != null ) {
			create( MAPS_ID, MockHelper.stringValueArray( "value", manyToOne.getMapsId() ) );
		}
		if ( manyToOne.isId() != null && manyToOne.isId() ) {
			create( ID );
		}
	}

	@Override
	protected JaxbAccessType getAccessType() {
		return manyToOne.getAccess();
	}

	@Override
	protected void setAccessType(JaxbAccessType accessType) {
		manyToOne.setAccess( accessType );
	}
}
