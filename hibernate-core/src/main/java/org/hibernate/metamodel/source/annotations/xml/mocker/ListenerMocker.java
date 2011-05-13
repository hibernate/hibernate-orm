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

import org.hibernate.metamodel.source.annotation.xml.XMLEntityListener;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityListeners;
import org.hibernate.metamodel.source.annotation.xml.XMLPostLoad;
import org.hibernate.metamodel.source.annotation.xml.XMLPostPersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPostRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPostUpdate;
import org.hibernate.metamodel.source.annotation.xml.XMLPrePersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPreRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPreUpdate;

/**
 * @author Strong Liu
 */
class ListenerMocker extends AbstractMocker {
	private ClassInfo classInfo;

	ListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo) {
		super( indexBuilder );
		this.classInfo = classInfo;
	}

	//@EntityListeners
	AnnotationInstance parser(XMLEntityListeners entityListeners) {
		if ( entityListeners == null ) {
			return null;
		}
		//class array value
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		List<String> clazzNameList = new ArrayList<String>();
		for ( XMLEntityListener listener : entityListeners.getEntityListener() ) {
			MockHelper.addToCollectionIfNotNull( clazzNameList, listener.getClazz() );
			parser( listener );

		}
		MockHelper.classArrayValue( "value", clazzNameList, annotationValueList, indexBuilder.getServiceRegistry() );
		return create( ENTITY_LISTENERS, classInfo, annotationValueList );
	}

	private void parser(XMLEntityListener listener) {
		String clazz = listener.getClazz();
		ClassInfo tempClassInfo = indexBuilder.createClassInfo( clazz );
		ListenerMocker builder = createListenerMocker( indexBuilder, tempClassInfo );
		builder.parser( listener.getPostLoad() );
		builder.parser( listener.getPostPersist() );
		builder.parser( listener.getPostRemove() );
		builder.parser( listener.getPostUpdate() );
		builder.parser( listener.getPrePersist() );
		builder.parser( listener.getPreRemove() );
		builder.parser( listener.getPreUpdate() );
		indexBuilder.finishEntityObject( tempClassInfo.name(), null );
	}
	protected ListenerMocker createListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo){
		return new ListenerMocker( indexBuilder, classInfo );
	}

	//@PrePersist
	AnnotationInstance parser(XMLPrePersist callback) {
		if ( callback == null ) {
			return null;
		}
		return create( PRE_PERSIST, getTarget( callback.getMethodName() ) );
	}

	//@PreRemove
	AnnotationInstance parser(XMLPreRemove callback) {
		if ( callback == null ) {
			return null;
		}
		return create( PRE_REMOVE, getTarget( callback.getMethodName() ) );
	}

	//@PreUpdate
	AnnotationInstance parser(XMLPreUpdate callback) {
		if ( callback == null ) {
			return null;
		}
		return create( PRE_UPDATE, getTarget( callback.getMethodName() ) );
	}

	//@PostPersist
	AnnotationInstance parser(XMLPostPersist callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_PERSIST, getTarget( callback.getMethodName() ) );
	}

	//@PostUpdate
	AnnotationInstance parser(XMLPostUpdate callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_UPDATE, getTarget( callback.getMethodName() ) );
	}

	//@PostRemove
	AnnotationInstance parser(XMLPostRemove callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_REMOVE, getTarget( callback.getMethodName() ) );
	}

	//@PostLoad
	AnnotationInstance parser(XMLPostLoad callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_LOAD, getTarget( callback.getMethodName() ) );
	}

	private AnnotationTarget getTarget(String methodName) {
		return MockHelper.getTarget(
				indexBuilder.getServiceRegistry(), classInfo, methodName, MockHelper.TargetType.METHOD
		);
	}

	@Override
	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null && annotationInstance.target() != null ) {
			indexBuilder.addAnnotationInstance( classInfo.name(), annotationInstance );
		}
		return annotationInstance;
	}
}
