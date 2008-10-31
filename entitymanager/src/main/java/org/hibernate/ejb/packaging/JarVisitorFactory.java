//$
package org.hibernate.ejb.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class JarVisitorFactory {
	private static final Logger log = LoggerFactory.getLogger( JarVisitorFactory.class );

	/**
	 * Get the JAR URL of the JAR containing the given entry
	 * Method used in a non managed environment
	 *
	 * @param url URL pointing to the known file in the JAR
	 * @param entry file known to be in the JAR
	 * @return the JAR URL
	 * @throws IllegalArgumentException if none URL is found
	 */
	public static URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		URL jarUrl;
		String file = url.getFile();
		if ( ! entry.startsWith( "/" ) ) entry = "/" + entry;
		file = file.substring( 0, file.length() - entry.length() );
		if ( file.endsWith( "!" ) ) file = file.substring( 0, file.length() - 1 );
		try {
			String protocol = url.getProtocol();

			if ( "jar".equals( protocol )
					|| "wsjar".equals( protocol ) ) { //Websphere has it's own way
				//Original URL is like jar:protocol
				jarUrl = new URL( file );
				if ( "file".equals( jarUrl.getProtocol() ) ) {
					//not escaped, need to voodoo
					if ( file.indexOf( ' ' ) != -1 ) {
						//not escaped, need to voodoo
						jarUrl = new File( jarUrl.getFile() ).toURI().toURL(); //goes by toURI to escape the path
					}
				} //otherwise left as is
			}
			else if ( "zip".equals( protocol ) //Weblogic has it's own way
					|| "code-source".equals( url.getProtocol() ) //OC4J prevent ejb.jar access (ie everything without path)
					|| "file".equals( protocol )  //if no wrapping is done
					) {
				//we have extracted the zip file, so it should be read as a file
				if ( file.indexOf( ' ' ) != -1 ) {
					//not escaped, need to voodoo
					jarUrl = new File(file).toURI().toURL(); //goes by toURI to escape the path
				}
				else {
					jarUrl = new File(file).toURL();
				}
			}
			else {
				jarUrl = new URL( protocol, url.getHost(), url.getPort(), file );
			}
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(
					"Unable to determine JAR Url from " + url + ". Cause: " + e.getMessage()
			);
		}
		log.trace("JAR URL from URL Entry: {} >> {}", url, jarUrl);
		return jarUrl;
	}

	/**
	 * Get a JarVisitor to the jar <code>jarPath</code> applying the given filters
	 *
	 * Method used in a non-managed environment
	 *
	 * @throws IllegalArgumentException if the jarPath is incorrect
	 */
	public static JarVisitor getVisitor(String jarPath, Filter[] filters) throws IllegalArgumentException {
		File file = new File( jarPath );
		if ( file.isFile() ) {
			return new InputStreamZippedJarVisitor( jarPath, filters );
		}
		else {
			return new ExplodedJarVisitor( jarPath, filters );
		}
	}

	/**
	 * Build a JarVisitor on the given JAR URL applying the given filters
	 *
	 * @throws IllegalArgumentException if the URL is malformed
	 */
	public static JarVisitor getVisitor(URL jarUrl, Filter[] filters) throws IllegalArgumentException {
		return getVisitor( jarUrl, filters, "" );
	}

	public static JarVisitor getVisitor(URL jarUrl, Filter[] filters, String entry) throws IllegalArgumentException {
		String protocol = jarUrl.getProtocol();
		if ( "jar".equals( protocol ) ) {
			return new JarProtocolVisitor( jarUrl, filters, entry );
		}
		else if ( StringHelper.isEmpty( protocol ) || "file".equals( protocol ) ) {
			File file;
			try {
				final String filePart = jarUrl.getFile();
				if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
					//unescaped (from the container), keep as is
					file = new File( jarUrl.getFile() );
				}
				else {
					file = new File( jarUrl.toURI().getSchemeSpecificPart() );
				}
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(
						"Unable to visit JAR " + jarUrl + ". Cause: " + e.getMessage(), e
				);
			}

			if ( file.isDirectory() ) {
				return new ExplodedJarVisitor( jarUrl, filters, entry );
			}
			else {
				return new FileZippedJarVisitor( jarUrl, filters, entry );
			}
		}
		else {
			//let's assume the url can return the jar as a zip stream
			return new InputStreamZippedJarVisitor( jarUrl, filters, entry );
		}
	}

	public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		int size;
		byte[] tmpByte = new byte[ 4096 ];
		byte[] entryBytes = new byte[0];
		for ( ; ; ) {
			size = inputStream.read( tmpByte );
			if ( size == -1 ) break;
			byte[] current = new byte[ entryBytes.length + size ];
			System.arraycopy( entryBytes, 0, current, 0, entryBytes.length );
			System.arraycopy( tmpByte, 0, current, entryBytes.length, size );
			entryBytes = current;
		}
		return entryBytes;
	}
}
