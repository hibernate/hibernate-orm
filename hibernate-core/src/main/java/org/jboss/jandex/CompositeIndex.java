/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.jboss.jandex;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Aggregates information from multiple {@link Index} instances.
 *
 * @author John Bailey
 * @author Steve Ebersole
 */
public class CompositeIndex implements IndexResult {
	private final List<Index> indexes;

	public CompositeIndex(Index... indexes) {
		this( Arrays.asList( indexes ) );
	}

	public CompositeIndex(List<Index> indexes) {
		this.indexes = indexes;
	}

	@Override
	public Collection<AnnotationInstance> getAnnotations(DotName annotationName) {
		final Set<AnnotationInstance> allInstances = new HashSet<AnnotationInstance>();
		for (Index index : indexes) {
			copy( index.getAnnotations( annotationName ), allInstances );
		}
		return Collections.unmodifiableSet( allInstances );
	}

	private <T> void copy(Collection<T> source, Collection<T> target) {
		if ( source != null ) {
			target.addAll( source );
		}
	}

	@Override
	public Collection<ClassInfo> getKnownClasses() {
		final List<ClassInfo> allKnown = new ArrayList<ClassInfo>();
		for ( Index index : indexes ) {
			copy( index.getKnownClasses(), allKnown );
		}
		return Collections.unmodifiableCollection( allKnown );
	}

	@Override
	public ClassInfo getClassByName(DotName className) {
		for ( Index index : indexes ) {
			final ClassInfo info = index.getClassByName( className );
			if ( info != null ) {
				return info;
			}
		}
		return null;
	}

	@Override
	public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		for ( Index index : indexes ) {
			copy( index.getKnownDirectSubclasses( className ), allKnown );
		}
		return Collections.unmodifiableSet( allKnown );
	}

	@Override
	public Set<ClassInfo> getAllKnownSubclasses(final DotName className) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		final Set<DotName> processedClasses = new HashSet<DotName>();
		getAllKnownSubClasses(className, allKnown, processedClasses);
		return allKnown;
	}

	private void getAllKnownSubClasses(DotName className, Set<ClassInfo> allKnown, Set<DotName> processedClasses) {
		final Set<DotName> subClassesToProcess = new HashSet<DotName>();
		subClassesToProcess.add(className);
		while (!subClassesToProcess.isEmpty()) {
			final Iterator<DotName> toProcess = subClassesToProcess.iterator();
			DotName name = toProcess.next();
			toProcess.remove();
			processedClasses.add(name);
			getAllKnownSubClasses(name, allKnown, subClassesToProcess, processedClasses);
		}
	}

	private void getAllKnownSubClasses(
			DotName name,
			Set<ClassInfo> allKnown,
			Set<DotName> subClassesToProcess,
			Set<DotName> processedClasses) {
		for ( Index index : indexes ) {
			final Collection<ClassInfo> list = index.getKnownDirectSubclasses( name );
			if ( list != null ) {
				for ( final ClassInfo clazz : list ) {
					final DotName className = clazz.name();
					if ( !processedClasses.contains( className ) ) {
						allKnown.add( clazz );
						subClassesToProcess.add( className );
					}
				}
			}
		}
	}

	@Override
	public Collection<ClassInfo> getKnownDirectImplementors(DotName className) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		for ( Index index : indexes ) {
			copy( index.getKnownDirectImplementors( className ), allKnown );
		}
		return Collections.unmodifiableSet(allKnown);	}

	@Override
	public Collection<ClassInfo> getAllKnownImplementors(DotName interfaceName) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		final Set<DotName> subInterfacesToProcess = new HashSet<DotName>();
		final Set<DotName> processedClasses = new HashSet<DotName>();
		subInterfacesToProcess.add( interfaceName );
		while ( !subInterfacesToProcess.isEmpty() ) {
			final Iterator<DotName> toProcess = subInterfacesToProcess.iterator();
			DotName name = toProcess.next();
			toProcess.remove();
			processedClasses.add( name );
			getKnownImplementors( name, allKnown, subInterfacesToProcess, processedClasses );
		}
		return allKnown;
	}

	private void getKnownImplementors(
			DotName name,
			Set<ClassInfo> allKnown,
			Set<DotName> subInterfacesToProcess,
			Set<DotName> processedClasses) {
		for (Index index : indexes) {
			final List<ClassInfo> list = index.getKnownDirectImplementors(name);
			if (list != null) {
				for (final ClassInfo clazz : list) {
					final DotName className = clazz.name();
					if (!processedClasses.contains(className)) {
						if ( Modifier.isInterface( clazz.flags() )) {
							subInterfacesToProcess.add(className);
						}
						else {
							if (!allKnown.contains(clazz)) {
								allKnown.add(clazz);
								processedClasses.add(className);
								getAllKnownSubClasses(className, allKnown, processedClasses);
							}
						}
					}
				}
			}
		}
	}
}
