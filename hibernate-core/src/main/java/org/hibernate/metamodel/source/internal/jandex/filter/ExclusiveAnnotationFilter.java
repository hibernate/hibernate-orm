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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.internal.jandex.MockHelper;

/**
 * @author Strong Liu
 */
class ExclusiveAnnotationFilter extends AbstractAnnotationFilter {

	public static ExclusiveAnnotationFilter INSTANCE = new ExclusiveAnnotationFilter();
	private final DotName[] targetNames;
	private final List<ExclusiveGroup> exclusiveGroupList;

	private ExclusiveAnnotationFilter() {
		this.exclusiveGroupList = new ArrayList<ExclusiveGroup>();
		fillExclusiveGroupList();
		Set<DotName> names = new HashSet<DotName>();
		for ( ExclusiveGroup group : exclusiveGroupList ) {
			names.addAll( group.getNames() );
		}
		targetNames = names.toArray( new DotName[names.size()] );
	}

	private void fillExclusiveGroupList() {
			ExclusiveGroup group = new ExclusiveGroup();
			group.add( ENTITY );
			group.add( MAPPED_SUPERCLASS );
			group.add( EMBEDDABLE );
			group.scope = Scope.TYPE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( SECONDARY_TABLES );
			group.add( SECONDARY_TABLE );
			group.scope = Scope.TYPE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( PRIMARY_KEY_JOIN_COLUMNS );
			group.add( PRIMARY_KEY_JOIN_COLUMN );
			group.scope = Scope.ATTRIBUTE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( SQL_RESULT_SET_MAPPING );
			group.add( SQL_RESULT_SET_MAPPINGS );
			group.scope = Scope.TYPE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( NAMED_NATIVE_QUERY );
			group.add( NAMED_NATIVE_QUERIES );
			group.scope = Scope.TYPE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( NAMED_QUERY );
			group.add( NAMED_QUERIES );
			group.scope = Scope.TYPE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( ATTRIBUTE_OVERRIDES );
			group.add( ATTRIBUTE_OVERRIDE );
			group.scope = Scope.ATTRIBUTE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( ASSOCIATION_OVERRIDE );
			group.add( ASSOCIATION_OVERRIDES );
			group.scope = Scope.ATTRIBUTE;
			exclusiveGroupList.add( group );

			group = new ExclusiveGroup();
			group.add( MAP_KEY_JOIN_COLUMN );
			group.add( MAP_KEY_JOIN_COLUMNS );
			group.scope = Scope.ATTRIBUTE;
			exclusiveGroupList.add( group );

	}

	@Override
	protected void overrideIndexedAnnotationMap(DotName annName, AnnotationInstance annotationInstance, Map<DotName, List<AnnotationInstance>> map) {
		ExclusiveGroup group = getExclusiveGroup( annName );
		if ( group == null ) {
			return;
		}
		AnnotationTarget target = annotationInstance.target();
		for ( DotName entityAnnName : group ) {
			if ( !map.containsKey( entityAnnName ) ) {
				continue;
			}
			switch ( group.scope ) {
				case TYPE:
					map.put( entityAnnName, Collections.<AnnotationInstance>emptyList() );
					break;
				case ATTRIBUTE:
					List<AnnotationInstance> indexedAnnotationInstanceList = map.get( entityAnnName );
					Iterator<AnnotationInstance> iter = indexedAnnotationInstanceList.iterator();
					while ( iter.hasNext() ) {
						AnnotationInstance ann = iter.next();
						if ( MockHelper.targetEquals( target, ann.target() ) ) {
							iter.remove();
						}
					}
					break;
			}
		}
	}

	@Override
	protected DotName[] targetAnnotation() {
		return targetNames;
	}

	private ExclusiveGroup getExclusiveGroup(DotName annName) {
		for ( ExclusiveGroup group : exclusiveGroupList ) {
			if ( group.contains( annName ) ) {
				return group;
			}
		}
		return null;
	}

	enum Scope {TYPE, ATTRIBUTE}

	private class ExclusiveGroup implements Iterable<DotName> {
		public Set<DotName> getNames() {
			return names;
		}

		private Set<DotName> names = new HashSet<DotName>();
		Scope scope = Scope.ATTRIBUTE;

		@Override
		public Iterator<DotName> iterator() {
			return names.iterator();
		}

		boolean contains(DotName name) {
			return names.contains( name );
		}

		void add(DotName name) {
			names.add( name );
		}
	}
}
