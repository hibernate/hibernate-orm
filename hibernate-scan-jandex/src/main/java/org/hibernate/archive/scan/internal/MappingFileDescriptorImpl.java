/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;

/**
 * @author Steve Ebersole
 */
public final class MappingFileDescriptorImpl
		implements MappingFileDescriptor, Serializable {
	private final String name;
	private final InputStreamAccess streamAccess;

	public MappingFileDescriptorImpl(String name, InputStreamAccess streamAccess) {
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
	public String toString() {
		return "MappingFileDescriptorImpl["
				+ "name=" + name + ", "
				+ "streamAccess=" + streamAccess + ']';
	}
}
