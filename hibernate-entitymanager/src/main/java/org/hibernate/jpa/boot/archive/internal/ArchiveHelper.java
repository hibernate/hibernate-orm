/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.boot.archive.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.jpa.boot.archive.spi.ArchiveException;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class ArchiveHelper {
	private static final Logger log = Logger.getLogger( ArchiveHelper.class );

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
		log.trace("JAR URL from URL Entry: " + url + " >> " + jarUrl);
		return jarUrl;
	}

	/**
	 * get the URL from a given path string
	 *
	 * @throws IllegalArgumentException is something goes wrong
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

	public static String unqualifiedJarFileName(URL jarUrl) {
		// todo : weak algorithm subject to AOOBE
		String fileName = jarUrl.getFile();
		int exclamation = fileName.lastIndexOf( "!" );
		if (exclamation != -1) {
			fileName = fileName.substring( 0, exclamation );
		}

		int slash = fileName.lastIndexOf( "/" );
		if ( slash != -1 ) {
			fileName = fileName.substring(
					fileName.lastIndexOf( "/" ) + 1,
					fileName.length()
			);
		}

		if ( fileName.length() > 4 && fileName.endsWith( "ar" ) && fileName.charAt( fileName.length() - 4 ) == '.' ) {
			fileName = fileName.substring( 0, fileName.length() - 4 );
		}

		return fileName;
	}

	public static byte[] getBytesFromInputStreamSafely(InputStream inputStream) {
		try {
			return getBytesFromInputStream( inputStream );
		}
		catch (IOException e) {
			throw new ArchiveException( "Unable to extract bytes from InputStream", e );
		}
	}

	public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		// Optimized by HHH-7835
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
