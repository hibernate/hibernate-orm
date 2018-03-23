/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.mocks.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Steve Ebersole
 */
public class MockHttpURLConnection extends HttpURLConnection {

	protected MockHttpURLConnection(URL url) {
		super( url );
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException( "not yet implemented" );
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}

}