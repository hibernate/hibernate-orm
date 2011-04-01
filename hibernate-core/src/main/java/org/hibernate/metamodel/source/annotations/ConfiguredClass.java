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

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;

/**
 * Represents an entity, mapped superclass or component configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClass {
	public static final DotName ENTITY = DotName.createSimple( Entity.class.getName() );
	public static final DotName HIBERNATE_ENTITY = DotName.createSimple( org.hibernate.annotations.Entity.class.getName() );
	public static final DotName MAPPED_SUPER_CLASS = DotName.createSimple( MappedSuperclass.class.getName() );

	private final ClassInfo classInfo;
	private final ConfiguredClassHierarchy hierarchy;

	public ConfiguredClass(ClassInfo info, ConfiguredClassHierarchy hierarchy) {
		this.classInfo = info;
		this.hierarchy = hierarchy;
		init();
	}

	private void init() {
		//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( classInfo, ENTITY );
		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation( classInfo, MAPPED_SUPER_CLASS );
		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation( classInfo, HIBERNATE_ENTITY );

		if ( jpaEntityAnnotation != null && mappedSuperClassAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @MappedSuperclass: "
							+ classInfo.name().toString()
			);
		}


	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{classInfo=" ).append( classInfo );
		sb.append( '}' );
		return sb.toString();
	}
}


