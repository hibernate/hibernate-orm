/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * @author Steve Ebersole
 */
public class UrlInputStreamAccess implements InputStreamAccess, Serializable {
	private final URL url;

	public UrlInputStreamAccess(URL url) {
		this.url = url;
	}

	@Override
	public String getStreamName() {
		return url.toExternalForm();
	}

	@Override
	public InputStream accessInputStream() {
		try {
			return url.openStream();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not open url stream : " + url.toExternalForm() );
		}
	}
}
