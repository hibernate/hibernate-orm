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

import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbManyToMany;
import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class ManyToManyMocker extends PropertyMocker {
	private final JaxbManyToMany manyToMany;

	ManyToManyMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults, JaxbManyToMany manyToMany) {
		super( indexBuilder, classInfo, defaults );
		this.manyToMany = manyToMany;
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return manyToMany;
	}

	@Override
	protected void doProcess() {
		if (manyToMany.getCollectionType() != null) {
			// TODO: Move this to a helper?
			String collectionTypeName = MockHelper.getCollectionType( manyToMany.getCollectionType().getName() );
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.stringValue( "type", collectionTypeName, annotationValueList );
			create( COLLECTION_TYPE, annotationValueList );
		}
		if (manyToMany.isInverse() == null || manyToMany.isInverse()) {
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.stringValue( "hbmKey", manyToMany.getHbmKey(), annotationValueList );
			create( INVERSE, getTarget(), annotationValueList );
		}
		if (manyToMany.getHbmFetchMode() != null) {
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.enumValue( "value", FETCH_MODE, manyToMany.getHbmFetchMode(), annotationValueList );
			create( FETCH, getTarget(), annotationValueList );
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue( "targetEntity", manyToMany.getTargetEntity(), annotationValueList, getDefaults(),
				indexBuilder.getServiceRegistry() );
		MockHelper.enumValue( "fetch", FETCH_TYPE, manyToMany.getFetch(), annotationValueList );
		MockHelper.stringValue( "mappedBy", manyToMany.getMappedBy(), annotationValueList );
		MockHelper.cascadeValue( "cascade", manyToMany.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( MANY_TO_MANY, annotationValueList );
		parseMapKeyClass( manyToMany.getMapKeyClass(), getTarget() );
		parseMapKeyType( manyToMany.getMapKeyType(), getTarget() );
		parseMapKeyTemporal( manyToMany.getMapKeyTemporal(), getTarget() );
		parseMapKeyEnumerated( manyToMany.getMapKeyEnumerated(), getTarget() );
		parseMapKey( manyToMany.getMapKey(), getTarget() );
		parseAttributeOverrides( manyToMany.getMapKeyAttributeOverride(), getTarget() );
		parseMapKeyJoinColumnList( manyToMany.getMapKeyJoinColumn(), getTarget() );
		parseOrderColumn( manyToMany.getOrderColumn(), getTarget() );
		parseJoinTable( manyToMany.getJoinTable(), getTarget() );
		if ( manyToMany.getOrderBy() != null ) {
			create( JPADotNames.ORDER_BY, MockHelper.stringValueArray( "value", manyToMany.getOrderBy() ) );
		}
		
		annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.cascadeValue( "value", manyToMany.getHbmCascade(), annotationValueList );
		create( CASCADE, getTarget(), annotationValueList );
	}
}
