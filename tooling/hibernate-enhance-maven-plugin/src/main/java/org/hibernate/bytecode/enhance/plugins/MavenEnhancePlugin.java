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
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * This plugin will enhance Entity objects.
 * 
 * @author Jeremy Whiting
 * @phase "compile"
 */
@Mojo ( name="enhance", defaultPhase = LifecyclePhase.COMPILE )
@Execute ( goal ="enhance" , phase = LifecyclePhase.COMPILE )
public class MavenEnhancePlugin extends AbstractMojo implements EnhancementContext {

	/**
	 * The contexts to use during enhancement.
	 */
	private List<File> classes = new ArrayList<File>();
	private ClassPool pool = new ClassPool( false );
    private final Enhancer enhancer = new Enhancer( this);

	private static final String CLASS_EXTENSION = ".class";

	@Parameter(property="dir", defaultValue="${project.build.outputDirectory}")
	private String dir = null;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Started enhance plugin....." );
		/** Perform a depth first search for files. */
		File root = new File( this.dir ); 
		walkDir( root );

		if ( 0 < classes.size() ) {
			for ( File file : classes ) {
				processClassFile(file);
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


    /**
     * Atm only process files annotated with either @Entity or @Embeddable
     * @param javaClassFile
     */
    private void processClassFile(File javaClassFile)
            throws MojoExecutionException {
		try {
			final CtClass ctClass = getClassPool().makeClass( new FileInputStream( javaClassFile ) );
            if(this.isEntityClass(ctClass))
                processEntityClassFile(javaClassFile, ctClass);
            else if(this.isCompositeClass(ctClass))
                processCompositeClassFile(javaClassFile, ctClass);

        }
        catch (IOException e) {
            throw new MojoExecutionException(
                    String.format( "Error processing included file [%s]", javaClassFile.getAbsolutePath() ), e );
        }
    }

    private void processEntityClassFile(File javaClassFile, CtClass ctClass ) {
        try {
            getLog().info( String.format("Processing Entity class file [%1$s].", ctClass.getName()) );
            byte[] result = enhancer.enhance( ctClass.getName(), ctClass.toBytecode() );
            if(result != null)
                writeEnhancedClass(javaClassFile, result);
        }
        catch (Exception e) {
            getLog().error( "Unable to enhance class [" + ctClass.getName() + "]", e);
            return;
        }
    }

    private void processCompositeClassFile(File javaClassFile, CtClass ctClass) {
        try {
            getLog().info( String.format("Processing Composite class file [%1$s].", ctClass.getName()) );
            byte[] result = enhancer.enhanceComposite(ctClass.getName(), ctClass.toBytecode());
            if(result != null)
                writeEnhancedClass(javaClassFile, result);
        }
        catch (Exception e) {
            getLog().error( "Unable to enhance class [" + ctClass.getName() + "]", e);
            return;
        }
    }

    private void writeEnhancedClass(File javaClassFile, byte[] result)
            throws MojoExecutionException {
        try {
			if ( javaClassFile.delete() ) {
                    if ( ! javaClassFile.createNewFile() ) {
                        getLog().error( "Unable to recreate class file [" + javaClassFile.getName() + "]");
                    }
            }
			else {
				getLog().error( "Unable to delete class file [" + javaClassFile.getName() + "]");
			}

			FileOutputStream outputStream = new FileOutputStream( javaClassFile, false );
			try {
				outputStream.write( result);
				outputStream.flush();
			}
			finally {
				try {
					outputStream.close();
				}
				catch ( IOException ignore) {
				}
			}
        }
        catch (FileNotFoundException ignore) {
            // should not ever happen because of explicit checks
        }
        catch (IOException e) {
            throw new MojoExecutionException(
                    String.format( "Error processing included file [%s]", javaClassFile.getAbsolutePath() ), e );
        }
    }

	private ClassPool getClassPool() {
		return this.pool;
	}

    private boolean shouldInclude(CtClass ctClass) {
		// we currently only handle entity enhancement
		return ctClass.hasAnnotation( Entity.class );
	}

	@Override
	public ClassLoader getLoadingClassLoader() {
		return getClass().getClassLoader();
	}

	@Override
	public boolean isEntityClass(CtClass classDescriptor) {
        return classDescriptor.hasAnnotation(Entity.class);
    }

	@Override
	public boolean isCompositeClass(CtClass classDescriptor) {
        return classDescriptor.hasAnnotation(Embeddable.class);
	}

	@Override
	public boolean doDirtyCheckingInline(CtClass classDescriptor) {
		return true;
	}

	@Override
	public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
		return true;
	}

	@Override
	public boolean isLazyLoadable(CtField field) {
		return true;
	}

	@Override
	public boolean isPersistentField(CtField ctField) {
		// current check is to look for @Transient
		return ! ctField.hasAnnotation( Transient.class );
	}

    @Override
    public boolean isMappedCollection(CtField field) {
        try {
            return (field.getAnnotation(OneToMany.class) != null ||
                    field.getAnnotation(ManyToMany.class) != null ||
                    field.getAnnotation(ElementCollection.class) != null);
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

	@Override
	public CtField[] order(CtField[] persistentFields) {
		// for now...
		return persistentFields;
		// eventually needs to consult the Hibernate metamodel for proper ordering
	}
}
