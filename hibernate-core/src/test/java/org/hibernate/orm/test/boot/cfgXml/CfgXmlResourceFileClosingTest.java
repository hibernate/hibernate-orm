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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that makes sure the input stream inside {@link ConfigLoader#loadConfigXmlFile(File)}
 * gets closed.
 *
 * @author Mehmet Ali Emektar(inspired from CfgXmlResourceNameClosingTest)
 */
@BaseUnitTest
@JiraKey( value = "HHH-12986" )
public class CfgXmlResourceFileClosingTest {

	private String GetCfgXmlResourceFilePath() {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = originalClassLoader.getResource(
				CfgXmlResourceFileClosingTest.class.getName().replace( '.', '/' ) + ".class"
		);

		if ( myUrl == null ) {
			fail( "Unable to setup packaging test : could not resolve 'known class' url" );
		}

		int index = -1;
		if ( myUrl.getFile().contains( "target" ) ) {
			// assume there's normally a /target
			index = myUrl.getFile().lastIndexOf( "target" );
		}
		else if ( myUrl.getFile().contains( "bin" ) ) {
			// if running in some IDEs, may be in /bin instead
			index = myUrl.getFile().lastIndexOf( "bin" );
		}
		else if ( myUrl.getFile().contains( "out/test" ) ) {
			// intellij... intellij sets up project outputs little different
			int outIndex = myUrl.getFile().lastIndexOf( "out/test" );
			index = myUrl.getFile().lastIndexOf( '/', outIndex + 1 );
		}

		if ( index < 0 ) {
			fail( "Unable to setup packaging test : could not interpret url" );
		}

		String baseDirPath = myUrl.getFile().substring( 0, index );
		return baseDirPath.concat( "target/resources/test/org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" );
	}

	@Test
	public void testStreamClosing() {
		String cfgXmlFilePath = GetCfgXmlResourceFilePath();
		File cfgXmlFile = new File(cfgXmlFilePath);

		final var classLoaderService = new LocalClassLoaderServiceImpl();
		final var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
				.applyClassLoaderService( classLoaderService )
				.build();
		final var serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrapServiceRegistry )
				.configure( cfgXmlFile )
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
}
