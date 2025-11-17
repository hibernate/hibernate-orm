/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;

/**
 * @author Steve Ebersole
 */
public record PackageDescriptorImpl
		(String name, InputStreamAccess streamAccess)
		implements PackageDescriptor, Serializable {


	@Override
	public boolean equals(Object object) {
		return this == object
			|| object instanceof PackageDescriptorImpl that && name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public InputStreamAccess getStreamAccess() {
		return streamAccess;
	}
}
