package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveException;
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
        MockitoAnnotations.initMocks(this);
        abstractScanner = new AbstractScannerImpl(archiveDescriptorFactory) {};

        // URLs (example persistence.xml location and a jar-file reference)
        jarFileTagContent = new URL("file:earEntities.jar");
        String earLibPUnitLocation = "file:/jboss-eap-4.3.0/jboss-as/server/default/tmp/deploy/" +
                        "tmp8928495357123867261example-ear.ear-contents/lib/earLibPUnit.jar";
        persistenceXMLRoot = new URL(earLibPUnitLocation).toURI().toURL();

        // Return a mock by default; tests override this for more specific behavior
        doReturn(mock(ArchiveDescriptor.class)).when(archiveDescriptorFactory)
                .buildArchiveDescriptor(any(URL.class));
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
        scanFindsNonRootUrlsFromDirectory(jarFileTagContent);
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
        doThrow(new ArchiveException("File does not exist.")).when(archiveDescriptorFactory).buildArchiveDescriptor(jarFileTagContent);

        // Should look relative to the persistence unit root next
        URL rootRelativeJARUrl = new URL(persistenceXMLRoot, jarFileTagContent.getFile());
        scanFindsNonRootUrlsFromDirectory(rootRelativeJARUrl);
    }

    /**
     * Ensure the implementation builds a descriptor based on the given URL and then visits
     * the descriptor.
     *
     * @param directoryAndFileName expected location to build the descriptor from
     */
    private void scanFindsNonRootUrlsFromDirectory(URL directoryAndFileName) throws Exception {
        // Arrange
        doReturn(descriptor).when(archiveDescriptorFactory).buildArchiveDescriptor(directoryAndFileName);
        ScanEnvironment scanEnvironment = getEnvironment();

        // Act
        abstractScanner.scan(scanEnvironment, new StandardScanOptions(), mock(ScanParameters.class));

        // Assert
        verify(descriptor).visitArchive(any(ArchiveContext.class));
    }

    private ScanEnvironment getEnvironment() throws Exception {
        ScanEnvironment jpaScanEnvironment = mock(ScanEnvironment.class);
        doReturn(Arrays.asList(jarFileTagContent)).when(jpaScanEnvironment).getNonRootUrls();
        doReturn(persistenceXMLRoot).when(jpaScanEnvironment).getRootUrl();

        return jpaScanEnvironment;
    }
}