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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Work on a JAR that can only be accessed through a inputstream
 * This is less efficient than the {@link FileZippedJarVisitor}
 *
 * @author Emmanuel Bernard
 */
public class InputStreamZippedJarVisitor extends AbstractJarVisitor {
	private final Logger log = LoggerFactory.getLogger( InputStreamZippedJarVisitor.class );
	private String entry;

	public InputStreamZippedJarVisitor(URL url, Filter[] filters, String entry) {
		super( url, filters );
		this.entry = entry;
	}

	public InputStreamZippedJarVisitor(String fileName, Filter[] filters) {
		super( fileName, filters );
	}

	protected void doProcessElements() throws IOException {
		JarInputStream jis;
		try {
			jis = new JarInputStream( jarUrl.openStream() );
		}
		catch (Exception ze) {
			//really should catch IOException but Eclipse is buggy and raise NPE...
			log.warn( "Unable to find file (ignored): " + jarUrl, ze );
			return;
		}
		if ( entry != null && entry.length() == 1 ) entry = null; //no entry
		if ( entry != null && entry.startsWith( "/" ) ) entry = entry.substring( 1 ); //remove '/' header

		JarEntry jarEntry;
		while ( ( jarEntry = jis.getNextJarEntry() ) != null ) {
			String name = jarEntry.getName();
			if ( entry != null && ! name.startsWith( entry ) ) continue; //filter it out
			if ( !jarEntry.isDirectory() ) {
				if ( name.equals( entry ) ) {
					//exact match, might be a nested jar entry (ie from jar:file:..../foo.ear!/bar.jar)
					/*
					 * This algorithm assumes that the zipped file is only the URL root (including entry), not just any random entry
					 */
					JarInputStream subJis = null;
					try {
						subJis = new JarInputStream( jis );
						ZipEntry subZipEntry = jis.getNextEntry();
						while (subZipEntry != null) {
							if ( ! subZipEntry.isDirectory() ) {
								//FIXME copy sucks
								byte[] entryBytes = JarVisitorFactory.getBytesFromInputStream( jis );
								String subname = subZipEntry.getName();
								if ( subname.startsWith( "/" ) ) subname = subname.substring( 1 );
								addElement(
										subname,
										new ByteArrayInputStream(entryBytes),
										new ByteArrayInputStream(entryBytes)
								);
							}
							subZipEntry = jis.getNextJarEntry();
						}
					}
					finally {
						if (subJis != null) subJis.close();
					}
				}
				else {
					byte[] entryBytes = JarVisitorFactory.getBytesFromInputStream( jis );
					//build relative name
					if (entry != null) name = name.substring( entry.length() );
					if ( name.startsWith( "/" ) ) name = name.substring( 1 );
					//this is bad cause we actually read everything instead of walking it lazily
					addElement(
							name,
							new ByteArrayInputStream( entryBytes ),
							new ByteArrayInputStream( entryBytes )
					);
				}
			}
		}
		jis.close();
	}
}
