package org.hibernate.bytecode.util;

import java.util.Set;
import java.util.HashSet;

/**
 * BasicClassFilter provides class filtering based on a series of packages to
 * be included and/or a series of explicit class names to be included.  If
 * neither is specified, then no restrictions are applied.
 *
 * @author Steve Ebersole
 */
public class BasicClassFilter implements ClassFilter {
	private final String[] includedPackages;
	private final Set includedClassNames = new HashSet();
	private final boolean isAllEmpty;

	public BasicClassFilter() {
		this( null, null );
	}

	public BasicClassFilter(String[] includedPackages, String[] includedClassNames) {
		this.includedPackages = includedPackages;
		if ( includedClassNames != null ) {
			for ( int i = 0; i < includedClassNames.length; i++ ) {
				this.includedClassNames.add( includedClassNames[i] );
			}
		}

		isAllEmpty = ( this.includedPackages == null || this.includedPackages.length == 0 )
		             && ( this.includedClassNames.isEmpty() );
	}

	public boolean shouldInstrumentClass(String className) {
		if ( isAllEmpty ) {
			return true;
		}
		else if ( includedClassNames.contains( className ) ) {
			return true;
		}
		else if ( isInIncludedPackage( className ) ) {
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isInIncludedPackage(String className) {
		if ( includedPackages != null ) {
			for ( int i = 0; i < includedPackages.length; i++ ) {
				if ( className.startsWith( includedPackages[i] ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
