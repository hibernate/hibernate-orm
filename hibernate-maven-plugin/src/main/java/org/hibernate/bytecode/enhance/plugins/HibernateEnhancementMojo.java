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
package org.hibernate.bytecode.enhance.plugins;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This plugin will enhance Entity objects.
 * 
 * @author Jeremy Whiting
 */
@Mojo(name = "enhance")
public class HibernateEnhancementMojo extends AbstractMojo {

	/**
	 * The contexts to use during enhancement.
	 */
	private List<File> classes = new ArrayList<File>();
	private ClassPool pool = new ClassPool( false );

	private static final String CLASS_EXTENSION = ".class";

	@Parameter(property="dir", defaultValue="${project.build.outputDirectory}")
	private String dir = null;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Started enhance plugin....." );
		/** Perform a depth first search for files. */
		File root = new File( this.dir ); 
		walkDir( root );

		Enhancer enhancer = new Enhancer( new EnhancementContext() {

			private ClassLoader overridden;

			public ClassLoader getLoadingClassLoader() {
				if ( null == this.overridden ) {
					return getClass().getClassLoader();
				}
				else {
					return this.overridden;
				}
			}

			public void setClassLoader(ClassLoader loader) {
				this.overridden = loader;
			}

			public boolean isEntityClass(CtClass classDescriptor) {
				return true;
			}

			public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
				return true;
			}

			public boolean isLazyLoadable(CtField field) {
				return true;
			}

			public boolean isCompositeClass(CtClass classDescriptor) {
				return false;
			}

			public boolean doDirtyCheckingInline(CtClass classDescriptor) {
				return false;
			}

			public CtField[] order(CtField[] fields) {
				// TODO: load ordering from configuration.
				return fields;
			}

			public boolean isPersistentField(CtField ctField) {
				return !ctField.hasAnnotation( Transient.class );
			}

		} );

		if ( 0 < classes.size() ) {
			for ( File file : classes ) {
				enhanceClass( enhancer, file );
			}
		}

		getLog().info( "Enhance plugin completed." );
	}

	/**
	 * Expects a directory.
	 */
	private void walkDir(File dir) {

		walkDir( dir, new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return ( pathname.isFile() && pathname.getName().endsWith( CLASS_EXTENSION ) );
			}
		}, new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return ( pathname.isDirectory() );
			}
		} );
	}

	private void walkDir(File dir, FileFilter classesFilter, FileFilter dirFilter) {
		File[] dirs = dir.listFiles( dirFilter );
		for ( int i = 0; i < dirs.length; i++ ) {
			walkDir( dirs[i], classesFilter, dirFilter );
		}
		dirs = null;
		File[] files = dir.listFiles( classesFilter );
		for ( int i = 0; i < files.length; i++ ) {
			this.classes.add( files[i] );
		}
	}

	private void enhanceClass(Enhancer enhancer, File file) {
		byte[] enhancedBytecode = null;
		InputStream is = null;
		CtClass clas = null;
		try {
			is = new FileInputStream( file.toString() );
			clas = getClassPool().makeClass( is );
			if ( !clas.hasAnnotation( Entity.class ) ) {
				getLog().debug( "Class $file not an annotated Entity class. skipping..." );
			}
			else {
				enhancedBytecode = enhancer.enhance( clas.getName(), clas.toBytecode() );
			}
		}
		catch (Exception e) {
			getLog().error( "Unable to enhance class [${file.toString()}]", e );
			return;
		}
		finally {
			try {
				if ( null != is )
					is.close();
			}
			catch (IOException ioe) {}
		}
		if ( null != enhancedBytecode ) {
			if ( file.delete() ) {
				try {
					if ( !file.createNewFile() ) {
						getLog().error( "Unable to recreate class file [" + clas.getName() + "]" );
					}
				}
				catch (IOException ioe) {
				}
			}
			else {
				getLog().error( "Unable to delete class file [" + clas.getName() + "]" );
			}
			FileOutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream( file, false );
				outputStream.write( enhancedBytecode );
				outputStream.flush();
			}
			catch (IOException ioe) {
			}
			finally {
				try {
					if ( outputStream != null )
						outputStream.close();
					clas.detach();// release memory
				}
				catch (IOException ignore) {
				}
			}
		}
	}

	public void setDir(String dir) {
		if ( null != dir && !"".equals( dir.trim() ) ) {
			this.dir = dir;
		}
	}

	private ClassPool getClassPool() {
		return this.pool;
	}

}
