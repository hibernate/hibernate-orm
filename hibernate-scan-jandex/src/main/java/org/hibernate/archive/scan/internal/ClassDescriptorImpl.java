/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * @author Steve Ebersole
 */
public class ClassDescriptorImpl implements ClassDescriptor, Serializable {
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
