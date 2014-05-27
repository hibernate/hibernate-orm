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
import org.hibernate.metamodel.source.internal.jaxb.JaxbOneToMany;
import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class OneToManyMocker extends PropertyMocker {
	private final JaxbOneToMany oneToMany;

	OneToManyMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults, JaxbOneToMany oneToMany) {
		super( indexBuilder, classInfo, defaults );
		this.oneToMany = oneToMany;
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return oneToMany;
	}

	@Override
	protected void doProcess() {
		if (oneToMany.getCollectionType() != null) {
			// TODO: Move this to a helper?
			String collectionTypeName = MockHelper.getCollectionType( oneToMany.getCollectionType().getName() );
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.stringValue( "type", collectionTypeName, annotationValueList );
			create( COLLECTION_TYPE, annotationValueList );
		}
		if (oneToMany.isInverse() != null && oneToMany.isInverse()) {
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			create( INVERSE, getTarget(), annotationValueList );
		}
		if (oneToMany.getHbmFetchMode() != null) {
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.enumValue( "value", FETCH_MODE, oneToMany.getHbmFetchMode(), annotationValueList );
			create( FETCH, getTarget(), annotationValueList );
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue( "targetEntity", oneToMany.getTargetEntity(), annotationValueList, getDefaults(),
				indexBuilder.getServiceRegistry() );
		MockHelper.enumValue( "fetch", FETCH_TYPE, oneToMany.getFetch(), annotationValueList );
		MockHelper.stringValue( "mappedBy", oneToMany.getMappedBy(), annotationValueList );
		MockHelper.booleanValue( "orphanRemoval", oneToMany.isOrphanRemoval(), annotationValueList );
		MockHelper.cascadeValue( "cascade", oneToMany.getCascade(), isDefaultCascadePersist(), annotationValueList );
		create( ONE_TO_MANY, getTarget(), annotationValueList );
		parseAttributeOverrides( oneToMany.getMapKeyAttributeOverride(), getTarget() );
		parseMapKeyJoinColumnList( oneToMany.getMapKeyJoinColumn(), getTarget() );
		parseMapKey( oneToMany.getMapKey(), getTarget() );
		parseMapKeyColumn( oneToMany.getMapKeyColumn(), getTarget() );
		parseMapKeyClass( oneToMany.getMapKeyClass(), getTarget() );
		parseMapKeyType( oneToMany.getMapKeyType(), getTarget() );
		parseMapKeyTemporal( oneToMany.getMapKeyTemporal(), getTarget() );
		parseMapKeyEnumerated( oneToMany.getMapKeyEnumerated(), getTarget() );
		parseJoinColumnList( oneToMany.getJoinColumn(), getTarget() );
		parseOrderColumn( oneToMany.getOrderColumn(), getTarget() );
		parseJoinTable( oneToMany.getJoinTable(), getTarget() );
		parseOnDelete( oneToMany.getOnDelete(), getTarget() );
		if ( oneToMany.getOrderBy() != null ) {
			create( JPADotNames.ORDER_BY, getTarget(), MockHelper.stringValueArray( "value", oneToMany.getOrderBy() ) );
		}
		
		annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.cascadeValue( "value", oneToMany.getHbmCascade(), annotationValueList );
		create( CASCADE, getTarget(), annotationValueList );
	}
}
