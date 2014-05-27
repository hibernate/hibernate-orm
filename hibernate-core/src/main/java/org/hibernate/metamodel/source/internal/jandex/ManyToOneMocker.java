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

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jaxb.JaxbManyToOne;
import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class ManyToOneMocker extends PropertyMocker {
	private final JaxbManyToOne manyToOne;

	ManyToOneMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults, JaxbManyToOne manyToOne) {
		super( indexBuilder, classInfo, defaults );
		this.manyToOne = manyToOne;
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return manyToOne;
	}

	@Override
	protected void doProcess() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue( "targetEntity",
				StringHelper.qualifyIfNot( getDefaults().getPackageName(), manyToOne.getTargetEntity() ),
				annotationValueList, getDefaults(), indexBuilder.getServiceRegistry() );
		MockHelper.enumValue( "fetch", FETCH_TYPE, manyToOne.getFetch(), annotationValueList );
		MockHelper.booleanValue( "optional", manyToOne.isOptional(), annotationValueList );
		MockHelper.cascadeValue( "cascade", manyToOne.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( MANY_TO_ONE, annotationValueList );

		parseJoinColumnList( manyToOne.getJoinColumn(), getTarget() );
		parseJoinTable( manyToOne.getJoinTable(), getTarget() );
		if ( manyToOne.getMapsId() != null ) {
			create( MAPS_ID, MockHelper.stringValueArray( "value", manyToOne.getMapsId() ) );
		}
		if ( manyToOne.isId() != null && manyToOne.isId() ) {
			create( ID );
		}
		
		annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.cascadeValue( "value", manyToOne.getHbmCascade(), annotationValueList );
		create( CASCADE, getTarget(), annotationValueList );
	}
}
