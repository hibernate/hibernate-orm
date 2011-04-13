/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.annotations;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final ClassInfo classToBind;

	public EntityBinder(MetadataImpl metadata, ClassInfo classInfo, AnnotationInstance jpaEntityAnnotation, AnnotationInstance hibernateEntityAnnotation) {
		this.classToBind = classInfo;
		EntityBinding entityBinding = new EntityBinding();
		bindJpaAnnotation( jpaEntityAnnotation, entityBinding );
		bindHibernateAnnotation( hibernateEntityAnnotation, entityBinding );
		metadata.addEntity( entityBinding );
	}

	private void bindHibernateAnnotation(AnnotationInstance annotation, EntityBinding entityBinding) {
//		if ( hibAnn != null ) {
//			dynamicInsert = hibAnn.dynamicInsert();
//			dynamicUpdate = hibAnn.dynamicUpdate();
//			optimisticLockType = hibAnn.optimisticLock();
//			selectBeforeUpdate = hibAnn.selectBeforeUpdate();
//			polymorphismType = hibAnn.polymorphism();
//			explicitHibernateEntityAnnotation = true;
//			//persister handled in bind
//		}
//		else {
//			//default values when the annotation is not there
//			dynamicInsert = false;
//			dynamicUpdate = false;
//			optimisticLockType = OptimisticLockType.VERSION;
//			polymorphismType = PolymorphismType.IMPLICIT;
//			selectBeforeUpdate = false;
//		}
	}

	private void bindJpaAnnotation(AnnotationInstance annotation, EntityBinding entityBinding) {
		if ( annotation == null ) {
			throw new AssertionFailure( "@Entity cannot be not null when binding an entity" );
		}
		String name;
		if ( annotation.value( "name" ) == null ) {
			name = StringHelper.unqualify( classToBind.name().toString() );
		}
		else {
			name = annotation.value( "name" ).asString();
		}
		entityBinding.setEntity( new Entity( name, null ) );
	}
}



