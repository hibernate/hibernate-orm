/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;

/**
 * @author Steve Ebersole
 */
public class PackageDescriptorImpl implements PackageDescriptor, Serializable {
	private final String name;
	private final InputStreamAccess streamAccess;

	public PackageDescriptorImpl(String name, InputStreamAccess streamAccess) {
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

		PackageDescriptorImpl that = (PackageDescriptorImpl) o;
		return name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
