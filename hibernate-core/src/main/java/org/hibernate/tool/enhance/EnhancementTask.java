/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.enhance;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ant task for performing build-time enhancement of entities and component/embeddable classes.
 * <p/>
 * IMPL NOTE : currently makes numerous assumptions, the most "horrific" being that all entities are
 * annotated @Entity which precludes {@code hbm.xml} mappings as well as complete {@code orm.xml} mappings.  This is
 * just a PoC though...
 *
 * @author Steve Ebersole
 * @see org.hibernate.engine.spi.Managed
 */
public class EnhancementTask extends Task implements EnhancementContext {
	private List<FileSet> filesets = new ArrayList<FileSet>();

	// Enhancer also builds CtClass instances.  Might make sense to share these (ClassPool).
	private final ClassPool classPool = new ClassPool( false );
	private final Enhancer enhancer = new Enhancer( this );

	public void addFileset(FileSet set) {
		this.filesets.add( set );
	}

	@Override
	public void execute() throws BuildException {
		log( "Starting Hibernate EnhancementTask execution", Project.MSG_INFO );

		// we use the CtClass stuff here just as a simple vehicle for obtaining low level information about
		// the class(es) contained in a file while still maintaining easy access to the underlying byte[]
		final Project project = getProject();

		for ( FileSet fileSet : filesets ) {
			final File fileSetBaseDir = fileSet.getDir( project );
			final DirectoryScanner directoryScanner = fileSet.getDirectoryScanner( project );
			for ( String relativeIncludedFileName : directoryScanner.getIncludedFiles() ) {
				final File javaClassFile = new File( fileSetBaseDir, relativeIncludedFileName );
				if ( !javaClassFile.exists() ) {
					continue;
				}

				processClassFile( javaClassFile );
			}
		}

	}

	/**
	 * Atm only process files annotated with either @Entity or @Embeddable
	 *
	 * @param javaClassFile
	 */
	private void processClassFile(File javaClassFile) {
		try {
			final CtClass ctClass = classPool.makeClass( new FileInputStream( javaClassFile ) );
			if ( this.isEntityClass( ctClass ) ) {
				processEntityClassFile( javaClassFile, ctClass );
			}
			else if ( this.isCompositeClass( ctClass ) ) {
				processCompositeClassFile( javaClassFile, ctClass );
			}

		}
		catch (IOException e) {
			throw new BuildException(
					String.format( "Error processing included file [%s]", javaClassFile.getAbsolutePath() ), e
			);
		}
	}

	private void processEntityClassFile(File javaClassFile, CtClass ctClass) {
		try {
			byte[] result = enhancer.enhance( ctClass.getName(), ctClass.toBytecode() );
			if ( result != null ) {
				writeEnhancedClass( javaClassFile, result );
			}
		}
		catch (Exception e) {
			log( "Unable to enhance class [" + ctClass.getName() + "]", e, Project.MSG_WARN );
		}
	}

	private void processCompositeClassFile(File javaClassFile, CtClass ctClass) {
		try {
			byte[] result = enhancer.enhanceComposite( ctClass.getName(), ctClass.toBytecode() );
			if ( result != null ) {
				writeEnhancedClass( javaClassFile, result );
			}
		}
		catch (Exception e) {
			log( "Unable to enhance class [" + ctClass.getName() + "]", e, Project.MSG_WARN );
		}
	}

	private void writeEnhancedClass(File javaClassFile, byte[] result) {
		try {
			if ( javaClassFile.delete() ) {
				if ( !javaClassFile.createNewFile() ) {
					log( "Unable to recreate class file [" + javaClassFile.getName() + "]", Project.MSG_INFO );
				}
			}
			else {
				log( "Unable to delete class file [" + javaClassFile.getName() + "]", Project.MSG_INFO );
			}

			FileOutputStream outputStream = new FileOutputStream( javaClassFile, false );
			try {
				outputStream.write( result );
				outputStream.flush();
			}
			finally {
				try {
					outputStream.close();
				}
				catch (IOException ignore) {
				}
			}
		}
		catch (FileNotFoundException ignore) {
			// should not ever happen because of explicit checks
		}
		catch (IOException e) {
			throw new BuildException(
					String.format( "Error processing included file [%s]", javaClassFile.getAbsolutePath() ), e
			);
		}
	}

	// EnhancementContext impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public ClassLoader getLoadingClassLoader() {
		return getClass().getClassLoader();
	}

	@Override
	public boolean isEntityClass(CtClass classDescriptor) {
		return classDescriptor.hasAnnotation( Entity.class );
	}

	@Override
	public boolean isCompositeClass(CtClass classDescriptor) {
		return classDescriptor.hasAnnotation( Embeddable.class );
	}

	@Override
	public boolean doBiDirectionalAssociationManagement(CtField field) {
		return false;
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
		return !ctField.hasAnnotation( Transient.class );
	}

	@Override
	public boolean isMappedCollection(CtField field) {
		try {
			return ( field.getAnnotation( OneToMany.class ) != null ||
					field.getAnnotation( ManyToMany.class ) != null ||
					field.getAnnotation( ElementCollection.class ) != null );
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
