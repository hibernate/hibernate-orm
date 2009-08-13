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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse a JAR of any form (zip file, exploded directory, ...)
 * apply a set of filters (File filter, Class filter, Package filter)
 * and return the appropriate matching sets of elements
 *
 * @author Emmanuel Bernard
 */
public abstract class AbstractJarVisitor implements JarVisitor {

	//TODO shortcut when filters are null or empty

	private final Logger log = LoggerFactory.getLogger( AbstractJarVisitor.class );
	protected String unqualifiedJarName;
	protected URL jarUrl;
	protected boolean done = false;
	private List<Filter> filters = new ArrayList<Filter>();
	private Set<FileFilter> fileFilters = new HashSet<FileFilter>();
	private Set<JavaElementFilter> classFilters = new HashSet<JavaElementFilter>();
	private Set<JavaElementFilter> packageFilters = new HashSet<JavaElementFilter>();
	private Set[] entries;



	/**
	 * Build a jar visitor from its jar string path
	 */
	private AbstractJarVisitor(String jarPath) {
		this.jarUrl = JarVisitorFactory.getURLFromPath( jarPath );
		unqualify();
	}

	protected AbstractJarVisitor(String fileName, Filter[] filters) {
		this( fileName );
		initFilters( filters );
	}

	private void initFilters(Filter[] filters) {
		for ( Filter filter : filters ) {
			if ( filter instanceof FileFilter ) {
				fileFilters.add( (FileFilter) filter );
			}
			else if ( filter instanceof ClassFilter ) {
				classFilters.add( (ClassFilter) filter );
			}
			else if ( filter instanceof PackageFilter ) {
				packageFilters.add( (PackageFilter) filter );
			}
			else {
				throw new AssertionError( "Unknown filter type: " + filter.getClass().getName() );
			}
			this.filters.add( filter );
		}
		int size = this.filters.size();
		this.entries = new Set[ size ];
		for ( int index = 0; index < size ; index++ ) {
			this.entries[index] = new HashSet<Entry>();
		}
	}

	protected AbstractJarVisitor(URL url, Filter[] filters) {
		this( url );
		initFilters( filters );
	}

	private AbstractJarVisitor(URL url) {
		jarUrl = url;
		unqualify();
	}

	protected void unqualify() {
		//FIXME weak algorithm subject to AOOBE
		String fileName = jarUrl.getFile();
		int exclamation = fileName.lastIndexOf( "!" );
		if (exclamation != -1) fileName = fileName.substring( 0, exclamation );
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
		unqualifiedJarName = fileName;
		log.debug( "Searching mapped entities in jar/par: {}", jarUrl );
	}

	/**
	 * Get the unqualified Jar name (ie wo path and wo extension)
	 */
	public String getUnqualifiedJarName() {
		return unqualifiedJarName;
	}

	public Filter[] getFilters() {
		return filters.toArray( new Filter[ filters.size() ] );
	}

	/**
	 * Return the matching entries for each filter in the same order the filter where passed
	 *
	 * @return array of Set of JarVisitor.Entry
	 * @throws IOException if something went wrong
	 */
	public Set[] getMatchingEntries() throws IOException {
		if ( !done ) {
			//avoid url access and so on
			if ( filters.size() > 0 ) doProcessElements();
			done = true;
		}
		return entries;
	}

	protected abstract void doProcessElements() throws IOException;

	//TODO avoid 2 input stream when not needed
	protected final void addElement(String entryName, InputStream is, InputStream secondIs) throws IOException {
		int entryNameLength = entryName.length();
		if ( entryName.endsWith( "package-info.class" ) ) {
			String name;
			if ( entryNameLength == "package-info.class".length() ) {
				name = "";
			}
			else {
				name = entryName.substring( 0, entryNameLength - ".package-info.class".length() ).replace( '/', '.' );
			}
			executeJavaElementFilter( name, packageFilters, is, secondIs );
		}
		else if ( entryName.endsWith( ".class" ) ) {
			String name = entryName.substring( 0, entryNameLength - ".class".length() ).replace( '/', '.' );
			log.debug( "Filtering: {}", name );
			executeJavaElementFilter( name, classFilters, is, secondIs );
		}
		else {
			String name = entryName;
			boolean accepted = false;
			for ( FileFilter filter : fileFilters ) {
				if ( filter.accept( name ) ) {
					accepted = true;
					InputStream localIs;
					if ( filter.getStream() ) {
						localIs = secondIs;
					}
					else {
						localIs = null;
						secondIs.close();
					}
					is.close();
					log.debug( "File Filter matched for {}", name );
					Entry entry = new Entry( name, localIs );
					int index = this.filters.indexOf( filter );
					this.entries[index].add( entry );
				}
			}
			if (!accepted) {
				//not accepted free resources
				is.close();
				secondIs.close();
			}
		}
	}

	private void executeJavaElementFilter(
			String name, Set<JavaElementFilter> filters, InputStream is, InputStream secondIs
	) throws IOException {
		boolean accepted = false;
		for ( JavaElementFilter filter : filters ) {
			if ( filter.accept( name ) ) {
				//FIXME cannot currently have a class filtered twice but matching once
				// need to copy the is
				boolean match = checkAnnotationMatching( is, filter );
				if ( match ) {
					accepted = true;
					InputStream localIs;
					if ( filter.getStream() ) {
						localIs = secondIs;
					}
					else {
						localIs = null;
						secondIs.close();
					}
					log.debug( "Java element filter matched for {}", name );
					Entry entry = new Entry( name, localIs );
					int index = this.filters.indexOf( filter );
					this.entries[index].add( entry );
					break; //we matched
				}
			}
		}
		if (!accepted) {
			is.close();
			secondIs.close();
		}
	}

	private boolean checkAnnotationMatching(InputStream is, JavaElementFilter filter) throws IOException {
		if ( filter.getAnnotations().length == 0 ) {
			is.close();
			return true;
		}
		DataInputStream dstream = new DataInputStream( is );
		ClassFile cf = null;

		try {
			cf = new ClassFile( dstream );
		}
		finally {
			dstream.close();
			is.close();
		}
		boolean match = false;
		AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute( AnnotationsAttribute.visibleTag );
		if ( visible != null ) {
			for ( Class annotation : filter.getAnnotations() ) {
				match = visible.getAnnotation( annotation.getName() ) != null;
				if ( match ) break;
			}
		}
		return match;
	}
}
