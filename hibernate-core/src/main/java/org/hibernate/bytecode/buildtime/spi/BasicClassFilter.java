/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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

	public BasicClassFilter() {
		this( null, null );
	}

	public BasicClassFilter(String[] includedPackages, String[] includedClassNames) {
		this.includedPackages = includedPackages;
		if ( includedClassNames != null ) {
			this.includedClassNames.addAll( Arrays.asList( includedClassNames ) );
		}

		isAllEmpty = ( this.includedPackages == null || this.includedPackages.length == 0 )
		             && ( this.includedClassNames.isEmpty() );
	}

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
