/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.cfgXml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test that makes sure the input stream inside {@link ConfigLoader#loadConfigXmlResource(java.lang.String)}
 * gets closed.
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-10120" )
public class CfgXmlResourceNameClosingTest extends BaseUnitTestCase {
	private static class InputStreamWrapper extends InputStream {
		private final InputStream wrapped;
		private boolean wasClosed = false;

		public InputStreamWrapper(InputStream wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int read() throws IOException {
			return wrapped.read();
		}

		@Override
		public void close() throws IOException {
			wrapped.close();
			wasClosed = true;
			super.close();
		}

		public boolean wasClosed() {
			return wasClosed;
		}
	}

	private static class LocalClassLoaderServiceImpl extends ClassLoaderServiceImpl {
		final List<InputStreamWrapper> openedStreams = new ArrayList<InputStreamWrapper>();
		boolean stopped = false;

		@Override
		public InputStream locateResourceStream(String name) {
			InputStreamWrapper stream = new InputStreamWrapper( super.locateResourceStream( name ) );
			openedStreams.add( stream );
			return stream;
		}

		@Override
		public void stop() {
			for ( InputStreamWrapper openedStream : openedStreams ) {
				if ( !openedStream.wasClosed ) {
					try {
						openedStream.close();
					}
					catch (IOException ignore) {
					}
				}
			}
			openedStreams.clear();
			stopped = true;
			super.stop();
		}
	}

	LocalClassLoaderServiceImpl classLoaderService = new LocalClassLoaderServiceImpl();

	@Test
	public void testStreamClosing() {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyClassLoaderService( classLoaderService )
				.build();
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.configure( "org/hibernate/test/boot/cfgXml/hibernate.cfg.xml" )
				.build();
		try {
			for ( InputStreamWrapper openedStream : classLoaderService.openedStreams ) {
				assertTrue( openedStream.wasClosed );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

		assertTrue( classLoaderService.stopped );
	}
}
