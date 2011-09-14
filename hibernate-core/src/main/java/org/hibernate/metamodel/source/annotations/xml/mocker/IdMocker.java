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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbGeneratedValue;
import org.hibernate.internal.jaxb.mapping.orm.JaxbId;

/**
 * @author Strong Liu
 */
class IdMocker extends PropertyMocker {
	private JaxbId id;

	IdMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbId id) {
		super( indexBuilder, classInfo, defaults );
		this.id = id;
	}

	@Override
	protected void processExtra() {
		create( ID );
		parserColumn( id.getColumn(), getTarget() );
		parserGeneratedValue( id.getGeneratedValue(), getTarget() );
		parserTemporalType( id.getTemporal(), getTarget() );
	}

	private AnnotationInstance parserGeneratedValue(JaxbGeneratedValue generatedValue, AnnotationTarget target) {
		if ( generatedValue == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "generator", generatedValue.getGenerator(), annotationValueList );
		MockHelper.enumValue(
				"strategy", GENERATION_TYPE, generatedValue.getStrategy(), annotationValueList
		);

		return create( GENERATED_VALUE, target, annotationValueList );
	}

	@Override
	protected String getFieldName() {
		return id.getName();
	}

	@Override
	protected JaxbAccessType getAccessType() {
		return id.getAccess();
	}

	@Override
	protected void setAccessType(JaxbAccessType accessType) {
		id.setAccess( accessType );
	}
}
