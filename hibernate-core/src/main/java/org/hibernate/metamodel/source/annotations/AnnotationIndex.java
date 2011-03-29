package org.hibernate.metamodel.source.annotations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

/**
 * Composite annotation index. Represents an annotation index for xml generated annotations and class level
 * defined annotations.
 *
 * @author John Bailey
 */
// TODO Use the composite index to abstract between xml and class source of annotations
public class AnnotationIndex {
	final Collection<Index> indexes;

	public AnnotationIndex(final Collection<Index> indexes) {
		this.indexes = indexes;
	}

	public AnnotationIndex(final AnnotationIndex... indexes) {
		this.indexes = new ArrayList<Index>();
		for ( AnnotationIndex index : indexes ) {
			this.indexes.addAll( index.indexes );
		}
	}

	/**
	 * @see {@link Index#getAnnotations(org.jboss.jandex.DotName)}
	 */
	public List<AnnotationInstance> getAnnotations(final DotName annotationName) {
		final List<AnnotationInstance> allInstances = new ArrayList<AnnotationInstance>();
		for ( Index index : indexes ) {
			final List<AnnotationInstance> list = index.getAnnotations( annotationName );
			if ( list != null ) {
				allInstances.addAll( list );
			}
		}
		return Collections.unmodifiableList( allInstances );
	}

	/**
	 * @see {@link Index#getKnownDirectSubclasses(org.jboss.jandex.DotName)}
	 */
	public Set<ClassInfo> getKnownDirectSubclasses(final DotName className) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		for ( Index index : indexes ) {
			final List<ClassInfo> list = index.getKnownDirectSubclasses( className );
			if ( list != null ) {
				allKnown.addAll( list );
			}
		}
		return Collections.unmodifiableSet( allKnown );
	}

	/**
	 * Returns all known subclasses of the given class, even non-direct sub classes. (i.e. it returns all known classes that are
	 * assignable to the given class);
	 *
	 * @param className The class
	 *
	 * @return All known subclasses
	 */
	public Set<ClassInfo> getAllKnownSubclasses(final DotName className) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		final Set<DotName> processedClasses = new HashSet<DotName>();
		getAllKnownSubClasses( className, allKnown, processedClasses );
		return allKnown;
	}

	private void getAllKnownSubClasses(DotName className, Set<ClassInfo> allKnown, Set<DotName> processedClasses) {
		final Set<DotName> subClassesToProcess = new HashSet<DotName>();
		subClassesToProcess.add( className );
		while ( !subClassesToProcess.isEmpty() ) {
			final Iterator<DotName> toProcess = subClassesToProcess.iterator();
			DotName name = toProcess.next();
			toProcess.remove();
			processedClasses.add( name );
			getAllKnownSubClasses( name, allKnown, subClassesToProcess, processedClasses );
		}
	}

	private void getAllKnownSubClasses(DotName name, Set<ClassInfo> allKnown, Set<DotName> subClassesToProcess,
									   Set<DotName> processedClasses) {
		for ( Index index : indexes ) {
			final List<ClassInfo> list = index.getKnownDirectSubclasses( name );
			if ( list != null ) {
				for ( final ClassInfo clazz : allKnown ) {
					final DotName className = clazz.name();
					if ( !processedClasses.contains( className ) ) {
						allKnown.add( clazz );
						subClassesToProcess.add( className );
					}
				}
			}
		}
	}

	/**
	 * @see {@link Index#getKnownDirectImplementors(DotName)}
	 */
	public Set<ClassInfo> getKnownDirectImplementors(final DotName className) {
		final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
		for ( Index index : indexes ) {
			final List<ClassInfo> list = index.getKnownDirectImplementors( className );
			if ( list != null ) {
				allKnown.addAll( list );
			}
		}
		return Collections.unmodifiableSet( allKnown );
	}

	/**
	 * Returns all known classes that implement the given interface, directly and indirectly. This will all return classes that
	 * implement sub interfaces of the interface, and sub classes of classes that implement the interface. (In short, it will
	 * return every class that is assignable to the interface that is found in the index)
	 * <p/>
	 * This will only return classes, not interfaces.
	 *
	 * @param interfaceName The interface
	 *
	 * @return All known implementors of the interface
	 */
	public Set<ClassInfo> getAllKnownImplementors(final DotName interfaceName) {
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

	private void getKnownImplementors(DotName name, Set<ClassInfo> allKnown, Set<DotName> subInterfacesToProcess,
									  Set<DotName> processedClasses) {
		for ( Index index : indexes ) {
			final List<ClassInfo> list = index.getKnownDirectImplementors( name );
			if ( list != null ) {
				for ( final ClassInfo clazz : allKnown ) {
					final DotName className = clazz.name();
					if ( !processedClasses.contains( className ) ) {
						if ( Modifier.isInterface( clazz.flags() ) ) {
							subInterfacesToProcess.add( className );
						}
						else {
							if ( !allKnown.contains( clazz ) ) {
								allKnown.add( clazz );
								processedClasses.add( className );
								getAllKnownSubClasses( className, allKnown, processedClasses );
							}
						}
					}
				}
			}
		}
	}

	/**
	 * @see {@link Index#getClassByName(org.jboss.jandex.DotName)}
	 */
	public ClassInfo getClassByName(final DotName className) {
		for ( Index index : indexes ) {
			final ClassInfo info = index.getClassByName( className );
			if ( info != null ) {
				return info;
			}
		}
		return null;
	}

	/**
	 * @see {@link org.jboss.jandex.Index#getKnownClasses()}
	 */
	public Collection<ClassInfo> getKnownClasses() {
		final List<ClassInfo> allKnown = new ArrayList<ClassInfo>();
		for ( Index index : indexes ) {
			final Collection<ClassInfo> list = index.getKnownClasses();
			if ( list != null ) {
				allKnown.addAll( list );
			}
		}
		return Collections.unmodifiableCollection( allKnown );
	}
}
