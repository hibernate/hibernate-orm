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
import org.hibernate.internal.jaxb.mapping.orm.JaxbBasic;

/**
 * @author Strong Liu
 */
class BasicMocker extends PropertyMocker {
	private JaxbBasic basic;

	BasicMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbBasic basic) {
		super( indexBuilder, classInfo, defaults );
		this.basic = basic;
	}

	@Override
	protected String getFieldName() {
		return basic.getName();
	}

	@Override
	protected void processExtra() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.booleanValue( "optional", basic.isOptional(), annotationValueList );
		MockHelper.enumValue( "fetch", FETCH_TYPE, basic.getFetch(), annotationValueList );
		create( BASIC, annotationValueList );
		parserColumn( basic.getColumn(), getTarget() );
		parserEnumType( basic.getEnumerated(), getTarget() );
		parserLob( basic.getLob(), getTarget() );
		parserTemporalType( basic.getTemporal(), getTarget() );

	}


	@Override
	protected JaxbAccessType getAccessType() {
		return basic.getAccess();
	}

	@Override
	protected void setAccessType(JaxbAccessType accessType) {
		basic.setAccess( accessType );
	}
}
