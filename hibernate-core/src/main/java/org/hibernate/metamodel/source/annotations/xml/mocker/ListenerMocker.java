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

import org.hibernate.MappingException;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityListener;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityListeners;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostLoad;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostPersist;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostRemove;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostUpdate;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPrePersist;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPreRemove;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPreUpdate;

/**
 * {@link javax.persistence.EntityListeners @EntityListeners} mocker
 *
 * @author Strong Liu
 */
class ListenerMocker extends AbstractMocker {
	private final ClassInfo classInfo;

	ListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo) {
		super( indexBuilder );
		this.classInfo = classInfo;
	}

	AnnotationInstance parser(JaxbEntityListeners entityListeners) {
		if ( entityListeners.getEntityListener().isEmpty() ) {
			throw new MappingException( "No child element of <entity-listener> found under <entity-listeners>." );
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>( 1 );
		List<String> clazzNameList = new ArrayList<String>( entityListeners.getEntityListener().size() );
		for ( JaxbEntityListener listener : entityListeners.getEntityListener() ) {
			MockHelper.addToCollectionIfNotNull( clazzNameList, listener.getClazz() );
			parserEntityListener( listener );
		}
		MockHelper.classArrayValue( "value", clazzNameList, annotationValueList, indexBuilder.getServiceRegistry() );
		return create( ENTITY_LISTENERS, classInfo, annotationValueList );
	}

	private void parserEntityListener(JaxbEntityListener listener) {
		String clazz = listener.getClazz();
		ClassInfo tempClassInfo = indexBuilder.createClassInfo( clazz );
		ListenerMocker mocker = createListenerMocker( indexBuilder, tempClassInfo );
		mocker.parser( listener.getPostLoad() );
		mocker.parser( listener.getPostPersist() );
		mocker.parser( listener.getPostRemove() );
		mocker.parser( listener.getPostUpdate() );
		mocker.parser( listener.getPrePersist() );
		mocker.parser( listener.getPreRemove() );
		mocker.parser( listener.getPreUpdate() );
		indexBuilder.finishEntityObject( tempClassInfo.name(), null );
	}

	protected ListenerMocker createListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo) {
		return new ListenerMocker( indexBuilder, classInfo );
	}

	//@PrePersist
	AnnotationInstance parser(JaxbPrePersist callback) {
		if ( callback == null ) {
			return null;
		}
		return create( PRE_PERSIST, getListenerTarget( callback.getMethodName() ) );
	}

	//@PreRemove
	AnnotationInstance parser(JaxbPreRemove callback) {
		if ( callback == null ) {
			return null;
		}
		return create( PRE_REMOVE, getListenerTarget( callback.getMethodName() ) );
	}

	//@PreUpdate
	AnnotationInstance parser(JaxbPreUpdate callback) {
		if ( callback == null ) {
			return null;
		}
		return create( PRE_UPDATE, getListenerTarget( callback.getMethodName() ) );
	}

	//@PostPersist
	AnnotationInstance parser(JaxbPostPersist callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_PERSIST, getListenerTarget( callback.getMethodName() ) );
	}

	//@PostUpdate
	AnnotationInstance parser(JaxbPostUpdate callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_UPDATE, getListenerTarget( callback.getMethodName() ) );
	}

	//@PostRemove
	AnnotationInstance parser(JaxbPostRemove callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_REMOVE, getListenerTarget( callback.getMethodName() ) );
	}

	//@PostLoad
	AnnotationInstance parser(JaxbPostLoad callback) {
		if ( callback == null ) {
			return null;
		}
		return create( POST_LOAD, getListenerTarget( callback.getMethodName() ) );
	}

	private AnnotationTarget getListenerTarget(String methodName) {
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
