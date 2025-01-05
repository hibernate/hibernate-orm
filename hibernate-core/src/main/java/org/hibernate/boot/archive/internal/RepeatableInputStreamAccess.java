/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;


import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jan Schatteman
 */
public class RepeatableInputStreamAccess implements InputStreamAccess {

	private final String resourceName;
	private byte[] bytes = new byte[0];

	public RepeatableInputStreamAccess(String resourceName, InputStream inputStream) {
		this.resourceName = resourceName;
		try {
			bytes = inputStream.readAllBytes();
		}
		catch (IOException | OutOfMemoryError e) {
			throw new HibernateException( "Could not read resource " + resourceName, e );
		}
	}

	@Override
	public String getStreamName() {
		return resourceName;
	}

	@Override
	public InputStream accessInputStream() {
		return new ByteArrayInputStream( bytes );
	}

}
