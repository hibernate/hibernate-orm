/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;

/**
 * @author Steve Ebersole
 */
public record PackageDescriptorImpl(String name, InputStreamAccess streamAccess)
		implements PackageDescriptor, Serializable {

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		PackageDescriptorImpl that = (PackageDescriptorImpl) o;
		return name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "PackageDescriptor(" + name + ")";
	}
}
