/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.archive.scan.internal;

import org.hibernate.metamodel.archive.scan.spi.ClassDescriptor;
import org.hibernate.metamodel.archive.spi.InputStreamAccess;

/**
 * @author Steve Ebersole
 */
public class ClassDescriptorImpl implements ClassDescriptor {
	private final String name;
	private final InputStreamAccess streamAccess;

	public ClassDescriptorImpl(String name, InputStreamAccess streamAccess) {
		this.name = name;
		this.streamAccess = streamAccess;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public InputStreamAccess getStreamAccess() {
		return streamAccess;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ClassDescriptorImpl that = (ClassDescriptorImpl) o;
		return name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
