/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package pkg.org.hibernate.jpa.test.packaging.temp_classloader;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;

/**
 * Out of org.hibernate package beacause ClassLoaderAccessImpl.isSafeClass would not load from temp classloader
 *
 * @author Janario Oliveira
 */
public class CustomAutoScanner extends StandardScanner {
	public static String AUTO_FOUND_JAR = "app-dep-custom-scanner.jar";

	@Override
	public ScanResult scan(
			final ScanEnvironment environment, ScanOptions options, ScanParameters parameters) {

		ScanEnvironment wrapped = new ScanEnvironment() {
			@Override
			public List<URL> getNonRootUrls() {
				List<URL> nonRootUrls = new ArrayList<>( environment.getNonRootUrls() );
				try {
					URL externalJar = CustomAutoScanner.class.getResource( "/" )
							.toURI().resolve( "../../packages/" + AUTO_FOUND_JAR ).toURL();
					nonRootUrls.add( externalJar );

					return nonRootUrls;
				}
				catch (URISyntaxException | MalformedURLException e) {
					throw new IllegalStateException( e );
				}
			}

			@Override
			public URL getRootUrl() {
				return environment.getRootUrl();
			}

			@Override
			public List<String> getExplicitlyListedClassNames() {
				return environment.getExplicitlyListedClassNames();
			}

			@Override
			public List<String> getExplicitlyListedMappingFiles() {
				return environment.getExplicitlyListedMappingFiles();
			}
		};

		return super.scan( wrapped, options, parameters );
	}
}
