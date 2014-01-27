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
package org.hibernate.metamodel.internal.source.annotations.xml.filter;

import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.xml.mocker.MockHelper;

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
			JPADotNames.LOB,
			JPADotNames.ID,
			JPADotNames.BASIC,
			JPADotNames.GENERATED_VALUE,
			JPADotNames.VERSION,
			JPADotNames.TRANSIENT,
			JPADotNames.ACCESS,
			JPADotNames.POST_LOAD,
			JPADotNames.POST_PERSIST,
			JPADotNames.POST_REMOVE,
			JPADotNames.POST_UPDATE,
			JPADotNames.PRE_PERSIST,
			JPADotNames.PRE_REMOVE,
			JPADotNames.PRE_UPDATE,
			JPADotNames.EMBEDDED_ID,
			JPADotNames.EMBEDDED,
			JPADotNames.MANY_TO_ONE,
			JPADotNames.MANY_TO_MANY,
			JPADotNames.ONE_TO_ONE,
			JPADotNames.ONE_TO_MANY,
			JPADotNames.ELEMENT_COLLECTION,
			JPADotNames.COLLECTION_TABLE,
			JPADotNames.COLUMN,
			JPADotNames.ENUMERATED,
			JPADotNames.JOIN_TABLE,
			JPADotNames.TEMPORAL,
			JPADotNames.ORDER_BY,
			JPADotNames.ORDER_COLUMN,
			JPADotNames.JOIN_COLUMN,
			JPADotNames.JOIN_COLUMNS,
			JPADotNames.MAPS_ID,
			JPADotNames.MAP_KEY_TEMPORAL,
			JPADotNames.MAP_KEY,
			JPADotNames.MAP_KEY_CLASS,
			JPADotNames.MAP_KEY_COLUMN,
			JPADotNames.MAP_KEY_ENUMERATED
	};
	@Override
	protected DotName[] targetAnnotation() {
		return TARGET_ANNOTATIONS;
	}
}
