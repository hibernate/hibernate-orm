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

import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.internal.jandex.MockHelper;

/**
 * @author Strong Liu
 */
class NameTargetAnnotationFilter extends AbstractAnnotationFilter {
	@Override
	protected void process(DotName annName, AnnotationInstance annotationInstance, List<AnnotationInstance> indexedAnnotationInstanceList) {
		AnnotationTarget target = annotationInstance.target();

		for ( Iterator<AnnotationInstance> iter = indexedAnnotationInstanceList.iterator(); iter.hasNext(); ) {
			AnnotationInstance ann = iter.next();
			if ( MockHelper.targetEquals( target, ann.target() ) ) {
				iter.remove();
			}
		}
	}

	public static NameTargetAnnotationFilter INSTANCE = new NameTargetAnnotationFilter();
	private static final DotName[] TARGET_ANNOTATIONS =  new DotName[] {
			LOB,
			ID,
			BASIC,
			GENERATED_VALUE,
			VERSION,
			TRANSIENT,
			ACCESS,
			POST_LOAD,
			POST_PERSIST,
			POST_REMOVE,
			POST_UPDATE,
			PRE_PERSIST,
			PRE_REMOVE,
			PRE_UPDATE,
			EMBEDDED_ID,
			EMBEDDED,
			MANY_TO_ONE,
			MANY_TO_MANY,
			ONE_TO_ONE,
			ONE_TO_MANY,
			ELEMENT_COLLECTION,
			COLLECTION_TABLE,
			COLUMN,
			ENUMERATED,
			JOIN_TABLE,
			TEMPORAL,
			ORDER_BY,
			ORDER_COLUMN,
			JOIN_COLUMN,
			JOIN_COLUMNS,
			MAPS_ID,
			MAP_KEY_TEMPORAL,
			MAP_KEY,
			MAP_KEY_CLASS,
			MAP_KEY_COLUMN,
			MAP_KEY_ENUMERATED
	};
	@Override
	protected DotName[] targetAnnotation() {
		return TARGET_ANNOTATIONS;
	}
}
