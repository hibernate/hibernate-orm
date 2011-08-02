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
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Represents the information about an entity annotated with {@code @Embeddable}.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddableClass extends ConfiguredClass {
	private final String embeddedAttributeName;
	private final String parentReferencingAttributeName;

	public EmbeddableClass(
			ClassInfo classInfo,
			String embeddedAttributeName,
			ConfiguredClass parent,
			AccessType defaultAccessType,
			AnnotationBindingContext context) {
		super( classInfo, defaultAccessType, parent, context );
		this.embeddedAttributeName = embeddedAttributeName;
		this.parentReferencingAttributeName = checkParentAnnotation();
	}

	private String checkParentAnnotation() {
		AnnotationInstance parentAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.PARENT
		);
		if ( parentAnnotation == null ) {
			return null;
		}
		else {
			return JandexHelper.getPropertyName( parentAnnotation.target() );
		}
	}

	public String getEmbeddedAttributeName() {
		return embeddedAttributeName;
	}

	public String getParentReferencingAttributeName() {
		return parentReferencingAttributeName;
	}
}


