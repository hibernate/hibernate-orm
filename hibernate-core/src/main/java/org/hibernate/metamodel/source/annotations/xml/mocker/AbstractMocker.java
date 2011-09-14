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
import org.jboss.jandex.DotName;

import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbUniqueConstraint;
import org.hibernate.metamodel.source.annotations.JPADotNames;

/**
 * Base class for the mock jandex annotations created from orm.xml.
 *
 * @author Strong Liu
 */
abstract class AbstractMocker implements JPADotNames {
	final protected IndexBuilder indexBuilder;

	AbstractMocker(IndexBuilder indexBuilder) {
		this.indexBuilder = indexBuilder;
	}


	abstract protected AnnotationInstance push(AnnotationInstance annotationInstance);


	protected AnnotationInstance create(DotName name, AnnotationTarget target) {
		return create( name, target, MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY );
	}


	protected AnnotationInstance create(DotName name, AnnotationTarget target, List<AnnotationValue> annotationValueList) {
		return create( name, target, MockHelper.toArray( annotationValueList ) );
	}

	protected AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] annotationValues) {
		AnnotationInstance annotationInstance = MockHelper.create( name, target, annotationValues );
		push( annotationInstance );
		return annotationInstance;

	}


	protected AnnotationInstance parserAccessType(JaxbAccessType accessType, AnnotationTarget target) {
		if ( accessType == null ) {
			return null;
		}
		return create( ACCESS, target, MockHelper.enumValueArray( "value", ACCESS_TYPE, accessType ) );
	}

	protected void nestedUniqueConstraintList(String name, List<JaxbUniqueConstraint> constraints, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parserUniqueConstraint( constraints.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}

	}

	//@UniqueConstraint
	protected AnnotationInstance parserUniqueConstraint(JaxbUniqueConstraint uniqueConstraint, AnnotationTarget target) {
		if ( uniqueConstraint == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", uniqueConstraint.getName(), annotationValueList );
		MockHelper.stringArrayValue( "columnNames", uniqueConstraint.getColumnName(), annotationValueList );
		return
				create( UNIQUE_CONSTRAINT, target,
						annotationValueList );
	}

}
