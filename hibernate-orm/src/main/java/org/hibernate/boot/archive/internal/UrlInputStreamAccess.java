/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.InputStream;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * @author Steve Ebersole
 */
public class UrlInputStreamAccess implements InputStreamAccess {
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
