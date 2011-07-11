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
package org.hibernate.metamodel.source.annotations.xml.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.annotations.xml.mocker.IndexBuilder;

/**
 * @author Strong Liu
 */
abstract class AbstractAnnotationFilter implements IndexedAnnotationFilter {
	protected static final DotName[] EMPTY_DOTNAME_ARRAY = new DotName[0];
	private Set<DotName> candidates;

	private boolean match(DotName annName) {
		if ( candidates == null ) {
			candidates = new HashSet<DotName>();
			candidates.addAll( Arrays.asList( targetAnnotation() ) );
		}
		return candidates.contains( annName );
	}

	@Override
	public void beforePush(IndexBuilder indexBuilder, DotName classDotName, AnnotationInstance annotationInstance) {
		DotName annName = annotationInstance.name();
		if ( !match( annName ) ) {
			return;
		}
		Map<DotName, List<AnnotationInstance>> map = indexBuilder.getIndexedAnnotations( classDotName );
		overrideIndexedAnnotationMap( annName, annotationInstance, map );
	}

	protected void overrideIndexedAnnotationMap(DotName annName, AnnotationInstance annotationInstance, Map<DotName, List<AnnotationInstance>> map) {
		if ( !map.containsKey( annName ) ) {
			return;
		}
		List<AnnotationInstance> indexedAnnotationInstanceList = map.get( annName );
		if ( indexedAnnotationInstanceList.isEmpty() ) {
			return;
		}
		process( annName, annotationInstance, indexedAnnotationInstanceList );
	}

	protected void process(DotName annName, AnnotationInstance annotationInstance, List<AnnotationInstance> indexedAnnotationInstanceList) {
	}

	protected DotName[] targetAnnotation() {
		return EMPTY_DOTNAME_ARRAY;
	}
}