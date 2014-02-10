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
package org.hibernate.metamodel.source.internal.jandex.filter;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * if class from index has matched annotations, then remove.
 *
 * @author Strong Liu
 */
class NameAnnotationFilter extends AbstractAnnotationFilter {
	@Override
	protected void process(DotName annName, AnnotationInstance annotationInstance, List<AnnotationInstance> indexedAnnotationInstanceList) {
		indexedAnnotationInstanceList.clear();
	}

	public static NameAnnotationFilter INSTANCE = new NameAnnotationFilter();
	private static final DotName[] TARGET_ANNOTATIONS = new DotName[] {
			CACHEABLE,
			TABLE,
			EXCLUDE_DEFAULT_LISTENERS,
			EXCLUDE_SUPERCLASS_LISTENERS,
			ID_CLASS,
			INHERITANCE,
			DISCRIMINATOR_VALUE,
			DISCRIMINATOR_COLUMN,
			ENTITY_LISTENERS
	};
	@Override
	protected DotName[] targetAnnotation() {
		return TARGET_ANNOTATIONS;
	}
}
