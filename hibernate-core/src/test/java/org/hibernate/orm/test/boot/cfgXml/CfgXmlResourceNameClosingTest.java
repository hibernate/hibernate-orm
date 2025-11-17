/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.cfgXml;

import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that makes sure the input stream inside {@link ConfigLoader#loadConfigXmlResource(String)}
 * gets closed.
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
@JiraKey( value = "HHH-10120" )
public class CfgXmlResourceNameClosingTest {
	@Test
	public void testStreamClosing() {
		final var classLoaderService = new LocalClassLoaderServiceImpl();
		final var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
				.applyClassLoaderService( classLoaderService )
				.build();
		final var serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrapServiceRegistry )
				.configure( "org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" )
				.build();
		try {
			assertThat( classLoaderService.openedStreams ).hasSize( 1 );
			for ( InputStreamWrapper openedStream : classLoaderService.openedStreams ) {
				assertTrue( openedStream.wasClosed );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		assertTrue( classLoaderService.stopped );
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


}
