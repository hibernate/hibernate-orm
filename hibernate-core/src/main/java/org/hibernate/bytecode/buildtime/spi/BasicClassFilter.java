/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.spi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * BasicClassFilter provides class filtering based on a series of packages to
 * be included and/or a series of explicit class names to be included.  If
 * neither is specified, then no restrictions are applied.
 *
 * @author Steve Ebersole
 */
public class BasicClassFilter implements ClassFilter {
	private final String[] includedPackages;
	private final Set<String> includedClassNames = new HashSet<String>();
	private final boolean isAllEmpty;

	/**
	 * Constructs a BasicClassFilter with given configuration.
	 */
	public BasicClassFilter() {
		this( null, null );
	}

	/**
	 * Constructs a BasicClassFilter with standard set of configuration.
	 *
	 * @param includedPackages Name of packages whose classes should be accepted.
	 * @param includedClassNames Name of classes that should be accepted.
	 */
	public BasicClassFilter(String[] includedPackages, String[] includedClassNames) {
		this.includedPackages = includedPackages;
		if ( includedClassNames != null ) {
			this.includedClassNames.addAll( Arrays.asList( includedClassNames ) );
		}

		isAllEmpty = ( this.includedPackages == null || this.includedPackages.length == 0 )
				&& ( this.includedClassNames.isEmpty() );
	}

	@Override
	public boolean shouldInstrumentClass(String className) {
		return isAllEmpty ||
				includedClassNames.contains( className ) ||
				isInIncludedPackage( className );
	}

	private boolean isInIncludedPackage(String className) {
		if ( includedPackages != null ) {
			for ( String includedPackage : includedPackages ) {
				if ( className.startsWith( includedPackage ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
