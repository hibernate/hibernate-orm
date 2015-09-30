/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * @author Steve Ebersole
 */
public class ClassDescriptorImpl implements ClassDescriptor {
	private final String name;
	private final Categorization categorization;
	private final InputStreamAccess streamAccess;

	public ClassDescriptorImpl(String name, Categorization categorization, InputStreamAccess streamAccess) {
		this.name = name;
		this.categorization = categorization;
		this.streamAccess = streamAccess;
	}

	@Override
	public String getName() {
		return name;
	}

	public Categorization getCategorization() {
		return categorization;
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
