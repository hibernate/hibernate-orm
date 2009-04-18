package org.hibernate.ejb.test.packaging;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import junit.framework.TestCase;

import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.Scanner;
import org.hibernate.ejb.test.pack.defaultpar.ApplicationServer;


/**
 * @author Emmanuel Bernard
 */
public class NativeScannerTest extends TestCase {
	private static final String jarFileBase = "file:./target/test-packages";

	public void testNativeScanner() throws Exception {
		String jarFileName = jarFileBase + "/defaultpar.par";
		Scanner scanner = new NativeScanner();

		final URL jarUrl = new URL( jarFileName );
		assertEquals( "defaultpar", scanner.getUnqualifiedJarName( jarUrl ) );

		Set<Class<? extends Annotation>> annotationsToLookFor = new HashSet<Class<? extends Annotation>>(3);
		annotationsToLookFor.add( Entity.class );
		annotationsToLookFor.add( MappedSuperclass.class );
		annotationsToLookFor.add( Embeddable.class );
		final Set<Class<?>> classes = scanner.getClassesInJar( jarUrl, annotationsToLookFor );
			
		assertEquals( 3, classes.size() );
		assertTrue( classes.contains( ApplicationServer.class ) );
		assertTrue( classes.contains( org.hibernate.ejb.test.pack.defaultpar.Version.class ) );

		Set<String> filePatterns = new HashSet<String>(2);
		filePatterns.add("**/*.hbm.xml");
		filePatterns.add("META-INF/orm.xml");
		final Set<NamedInputStream> files = scanner.getFilesInJar( jarUrl, filePatterns );

		assertEquals( 2, files.size() );
		for (NamedInputStream file : files ) {
			assertNotNull( file.getStream() );
			file.getStream().close();
		}
	}
}
