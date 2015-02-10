/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.boot.scan.spi;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.archive.spi.ArchiveException;
import org.hibernate.jpa.boot.scan.internal.StandardScanOptions;
import org.hibernate.jpa.boot.scan.spi.AbstractScannerImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Arrays;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AbstractScannerImplTest {

	private AbstractScannerImpl abstractScanner;
	private URL jarFileTagContent;
	private URL persistenceXMLRoot;

	@Mock private ArchiveDescriptorFactory archiveDescriptorFactory;
	@Mock private ArchiveDescriptor descriptor;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks( this);
		abstractScanner = new AbstractScannerImpl(archiveDescriptorFactory) {};

		// URLs (example persistence.xml location and a jar-file reference)
		jarFileTagContent = new URL("file:earEntities.jar");
		final String earLibPUnitLocation = "file:/jboss-eap-4.3.0/jboss-as/server/default/tmp/deploy/" +
						"tmp8928495357123867261example-ear.ear-contents/lib/earLibPUnit.jar";
		persistenceXMLRoot = new URL(earLibPUnitLocation).toURI().toURL();

		// Return a mock by default; tests override this for more specific behavior
		doReturn( mock( ArchiveDescriptor.class)).when( archiveDescriptorFactory)
				.buildArchiveDescriptor( any( URL.class));
	}

	/**
	 * <p>
	 * When searching for otherModule.jar based on a <pre>{@code <jar-file>otherModule.jar</jar-file>}</pre>
	 * markup in persistence.xml, default behavior for Hibernate 4.3.8-FINAL
	 * was to look for otherModule.jar in the JVM working directory. This behavior
	 * should be preserved to avoid regressions in production systems.
	 * </p>
	 */
	@Test
	public void scanFindsNonRootUrlsFromJVMWorkingDirectory() throws Exception {
		scanFindsNonRootUrlsFromDirectory( jarFileTagContent);
	}

	/**
	 * <p>
	 * When searching for otherModule.jar based on a <pre>{@code <jar-file>otherModule.jar</jar-file>}</pre>
	 * markup in persistence.xml, Hibernate should look for otherModule.jar relative
	 * to the root of the persistence unit (according to the JSR220 spec).
	 * </p>
	 *
	 * @see <a href="https://hibernate.atlassian.net/browse/HHH-4161">persistence.xml &lt;jar-file&gt; not following JSR220 spec</a>
	 */
	@Test
	public void scanFindsNonRootUrlsFromPersistenceXmlDirectory() throws Exception {
		// Attempt to find the JAR in the JVM working directory fails
		doThrow( new ArchiveException( "File does not exist.")).when( archiveDescriptorFactory).buildArchiveDescriptor( jarFileTagContent);

		// Should look relative to the persistence unit root next
		final URL rootRelativeJARUrl = new URL(persistenceXMLRoot, jarFileTagContent.getFile());
		scanFindsNonRootUrlsFromDirectory( rootRelativeJARUrl);
	}

	/**
	 * Ensure the implementation builds a descriptor based on the given URL and then visits
	 * the descriptor.
	 *
	 * @param directoryAndFileName expected location to build the descriptor from
	 */
	private void scanFindsNonRootUrlsFromDirectory(URL directoryAndFileName) throws Exception {
		// Arrange
		doReturn( descriptor).when( archiveDescriptorFactory).buildArchiveDescriptor( directoryAndFileName);
		final PersistenceUnitDescriptor persistenceUnitDescriptor = getPersistenceUnitDescriptor();

		// Act
		abstractScanner.scan( persistenceUnitDescriptor, new StandardScanOptions());

		// Assert
		verify( descriptor).visitArchive( any( ArchiveContext.class));
	}

	private PersistenceUnitDescriptor getPersistenceUnitDescriptor() throws Exception {
		final PersistenceUnitDescriptor persistenceUnitDescriptor = mock( PersistenceUnitDescriptor.class);
		doReturn( Arrays.asList( jarFileTagContent)).when( persistenceUnitDescriptor).getJarFileUrls();
		doReturn( persistenceXMLRoot).when( persistenceUnitDescriptor).getPersistenceUnitRootUrl();

		return persistenceUnitDescriptor;
	}
}
