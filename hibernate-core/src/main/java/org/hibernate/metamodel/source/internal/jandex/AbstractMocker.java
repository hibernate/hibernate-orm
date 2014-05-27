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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AccessType;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIndex;
import org.hibernate.metamodel.source.internal.jaxb.JaxbUniqueConstraint;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * Base class for the mock jandex annotations created from orm.xml.
 *
 * @author Strong Liu
 */
public abstract class AbstractMocker implements JPADotNames, HibernateDotNames {
	final protected IndexBuilder indexBuilder;
	final private Default defaults;

	AbstractMocker(IndexBuilder indexBuilder, Default defaults) {
		this.indexBuilder = indexBuilder;
		this.defaults = defaults;
	}

	protected Default getDefaults() {
		return defaults;
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


	protected AnnotationInstance parseAccessType(AccessType accessType, AnnotationTarget target) {
		if ( accessType == null ) {
			return null;
		}
		return create( ACCESS, target, MockHelper.enumValueArray( "value", ACCESS_TYPE, accessType ) );
	}

	protected void nestedUniqueConstraintList(String name, List<JaxbUniqueConstraint> constraints, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parseUniqueConstraint( constraints.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			MockHelper.addToCollectionIfNotNull( annotationValueList, AnnotationValue.createArrayValue( name, values ) );
		}

	}

	// @UniqueConstraint
	protected AnnotationInstance parseUniqueConstraint(JaxbUniqueConstraint uniqueConstraint, AnnotationTarget target) {
		if ( uniqueConstraint == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", uniqueConstraint.getName(), annotationValueList );
		MockHelper.stringArrayValue( "columnNames", uniqueConstraint.getColumnName(), annotationValueList );
		return create( UNIQUE_CONSTRAINT, target, annotationValueList );
	}

	protected void nestedIndexConstraintList(String name, List<JaxbIndex> constraints, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parseIndexConstraint( constraints.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			MockHelper.addToCollectionIfNotNull( annotationValueList, AnnotationValue.createArrayValue( name, values ) );
		}

	}

	// @Index
	protected AnnotationInstance parseIndexConstraint(JaxbIndex index, AnnotationTarget target) {
		if ( index == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", index.getName(), annotationValueList );
		MockHelper.stringValue( "columnList", index.getColumnList(), annotationValueList );
		MockHelper.booleanValue( "unique", index.isUnique(), annotationValueList );
		return create( INDEX, target, annotationValueList );
	}

}
