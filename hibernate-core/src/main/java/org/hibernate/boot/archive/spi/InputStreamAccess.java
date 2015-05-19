/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

import java.io.InputStream;

/**
 * Contract for building InputStreams, especially in on-demand situations
 *
 * @author Steve Ebersole
 */
public interface InputStreamAccess {
	/**
	 * Get the name of the resource backing the stream
	 *
	 * @return The backing resource name
	 */
	public String getStreamName();

	/**
	 * Get access to the stream.  Can be called multiple times, a different stream instance should be returned each time.
	 *
	 * @return The stream
	 */
	public InputStream accessInputStream();
}
