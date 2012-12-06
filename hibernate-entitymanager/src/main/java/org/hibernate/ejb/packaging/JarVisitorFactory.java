/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class JarVisitorFactory {

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                           JarVisitorFactory.class.getName());

	/**
	 * Get the JAR URL of the JAR containing the given entry
	 * Method used in a non managed environment
	 *
	 * @param url URL pointing to the known file in the JAR
	 * @param entry file known to be in the JAR
	 * @return the JAR URL
	 * @throws IllegalArgumentException if none URL is found
	 * TODO move to a ScannerHelper service?
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
				try {
					//We reconstruct the URL probably to make it work in some specific environments
					//Forgot the exact details, sorry (and the Git history does not help)
					jarUrl = new URL( protocol, url.getHost(), url.getPort(), file );
				}
				//HHH-6442: Arquilian
				catch ( final MalformedURLException murle ) {
					//Just use the provided URL as-is, likely it has a URLStreamHandler
					//associated w/ the instance
					jarUrl = url;
				}
			}
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(
					"Unable to determine JAR Url from " + url + ". Cause: " + e.getMessage()
			);
		}
        LOG.trace("JAR URL from URL Entry: " + url + " >> " + jarUrl);
		return jarUrl;
	}

	/**
	 * get the URL from a given path string
	 *
	 * @throws IllegalArgumentException is something goes wrong
	 * TODO move to a ScannerHelper service?
	 */
	public static URL getURLFromPath(String jarPath) {
		URL jarUrl;
		try {
			//is it an url
			jarUrl = new URL( jarPath );
		}
		catch ( MalformedURLException e) {
			try {
				//consider it as a file path
				jarUrl = new URL( "file:" + jarPath );
			}
			catch (MalformedURLException ee) {
				throw new IllegalArgumentException( "Unable to find jar:" + jarPath, ee );
			}
		}
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
		else if ( StringHelper.isEmpty( protocol ) || "file".equals( protocol ) || "vfszip".equals( protocol ) || "vfsfile".equals( protocol ) ) {
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

	// Optimized by HHH-7835
	public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		int size;
		List<byte[]> data = new LinkedList<byte[]>();
		int bufferSize = 4096;
		byte[] tmpByte = new byte[bufferSize];
		int offset = 0;
		int total = 0;
		for ( ;; ) {
			size = inputStream.read( tmpByte, offset, bufferSize - offset );
			if ( size == -1 )
				break;

			offset += size;

			if ( offset == tmpByte.length ) {
				data.add( tmpByte );
				tmpByte = new byte[bufferSize];
				offset = 0;
				total += tmpByte.length;
			}
		}

		byte[] result = new byte[total + offset];
		int count = 0;
		for ( byte[] arr : data ) {
			System.arraycopy( arr, 0, result, count * arr.length, arr.length );
			count++;
		}
		System.arraycopy( tmpByte, 0, result, count * tmpByte.length, offset );

		return result;
	}
}
