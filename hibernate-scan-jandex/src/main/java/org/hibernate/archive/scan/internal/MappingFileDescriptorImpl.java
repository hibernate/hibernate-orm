/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import java.io.Serializable;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;

/**
 * @author Steve Ebersole
 */
public class MappingFileDescriptorImpl implements MappingFileDescriptor, Serializable {
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

//	@Override
//	public boolean equals(Object o) {
//		if ( this == o ) {
//			return true;
//		}
//		if ( o == null || getClass() != o.getClass() ) {
//			return false;
//		}
//
//		MappingFileDescriptorImpl that = (MappingFileDescriptorImpl) o;
//
//		return name.equals( that.name )
//				&& streamAccess.getStreamName().equals( that.streamAccess.getStreamName() );
//
//	}
//
//	@Override
//	public int hashCode() {
//		int result = name.hashCode();
//		result = 31 * result + streamAccess.getStreamName().hashCode();
//		return result;
//	}
}
