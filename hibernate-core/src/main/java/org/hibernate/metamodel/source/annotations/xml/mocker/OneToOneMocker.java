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
import org.hibernate.internal.jaxb.mapping.orm.JaxbOneToOne;

/**
 * @author Strong Liu
 */
class OneToOneMocker extends PropertyMocker {
	private JaxbOneToOne oneToOne;

	OneToOneMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbOneToOne oneToOne) {
		super( indexBuilder, classInfo, defaults );
		this.oneToOne = oneToOne;
	}

	@Override
	protected String getFieldName() {
		return oneToOne.getName();
	}

	@Override
	protected void processExtra() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue(
				"targetEntity", oneToOne.getTargetEntity(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		MockHelper.enumValue( "fetch", FETCH_TYPE, oneToOne.getFetch(), annotationValueList );
		MockHelper.booleanValue( "optional", oneToOne.isOptional(), annotationValueList );
		MockHelper.booleanValue( "orphanRemoval", oneToOne.isOrphanRemoval(), annotationValueList );
		MockHelper.stringValue( "mappedBy", oneToOne.getMappedBy(), annotationValueList );
		MockHelper.cascadeValue( "cascade", oneToOne.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( ONE_TO_ONE, annotationValueList );

		parserPrimaryKeyJoinColumnList( oneToOne.getPrimaryKeyJoinColumn(), getTarget() );
		parserJoinColumnList( oneToOne.getJoinColumn(), getTarget() );
		parserJoinTable( oneToOne.getJoinTable(), getTarget() );
		if ( oneToOne.getMapsId() != null ) {
			create( MAPS_ID, MockHelper.stringValueArray( "value", oneToOne.getMapsId() ) );
		}
		if ( oneToOne.isId() != null && oneToOne.isId() ) {
			create( ID );
		}
	}

	@Override
	protected JaxbAccessType getAccessType() {
		return oneToOne.getAccess();
	}

	@Override
	protected void setAccessType(JaxbAccessType accessType) {
		oneToOne.setAccess( accessType );
	}
}
