/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * @author Steve Ebersole
 */
public record ClassDescriptorImpl
		(String name, Categorization categorization, InputStreamAccess streamAccess)
		implements ClassDescriptor, Serializable {

	@Override
	public boolean equals(Object object) {
		return this == object
			|| object instanceof ClassDescriptorImpl that && name.equals( that.name );
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
	public Categorization getCategorization() {
		return categorization;
	}

	@Override
	public InputStreamAccess getStreamAccess() {
		return streamAccess;
	}
}
