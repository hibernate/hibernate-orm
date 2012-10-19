/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.enhance;

import javax.persistence.Entity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * Ant task for performing build-time enhancement of entities and component/embeddable classes.
 * <p/>
 * IMPL NOTE : currently makes numerous assumptions, the most "horrific" being that all entities are
 * annotated @Entity which precludes {@code hbm.xml} mappings as well as complete {@code orm.xml} mappings.  This is
 * just a PoC though...
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.engine.spi.Managed
 */
public class EnhancementTask extends Task {
	private List<FileSet> filesets = new ArrayList<FileSet>();

	public void addFileset(FileSet set) {
		this.filesets.add( set );
	}

	@Override
	public void execute() throws BuildException {
		EnhancementContext enhancementContext = new EnhancementContext() {
			@Override
			public boolean isEntityClass(String className) {
				// currently we only call enhance on the classes with @Entity, so here we always return true
				return true;
			}

			@Override
			public boolean isCompositeClass(String className) {
				return false;
			}
		};

		// we use the CtClass stuff here just as a simple vehicle for obtaining low level information about
		// the class(es) contained in a file while still maintaining easy access to the underlying byte[]
		//
		// Enhancer also builds CtClass instances.  Might make sense to share these (ClassPool).
		final ClassPool classPool = new ClassPool( false );
		final List<CtClass> ctClassList = collectCtClasses( classPool );

		final Enhancer enhancer = new Enhancer( enhancementContext );
		for ( CtClass ctClass : ctClassList ) {
			try {
				enhancer.enhance( ctClass.getName(), ctClass.toBytecode() );
			}
			catch (Exception e) {
				log(
						"Unable to enhance class : " + ctClass.getName(),
						e,
						Project.MSG_WARN
				);
			}

		}
	}

	private List<CtClass> collectCtClasses(ClassPool classPool) {
		final List<CtClass> ctClassList = new ArrayList<CtClass>();

		final Project project = getProject();
		for ( FileSet fileSet : filesets ) {
			final File fileSetBaseDir = fileSet.getDir( project );
			final DirectoryScanner directoryScanner = fileSet.getDirectoryScanner( project );
			for ( String relativeIncludedFileName : directoryScanner.getIncludedFiles() ) {
				final File javaClassFile = new File( fileSetBaseDir, relativeIncludedFileName );
				if ( ! javaClassFile.exists() ) {
					continue;
				}
				try {
					final CtClass ctClass = classPool.makeClass( new FileInputStream( javaClassFile ) );
					collectCtClasses( ctClassList, ctClass );
				}
				catch (FileNotFoundException ignore) {
					// should not ever happen because of explicit check above
				}
				catch (IOException e) {
					throw new BuildException(
							String.format(
									"Error processing included file [%s : %s]",
									fileSetBaseDir.getAbsolutePath(),
									relativeIncludedFileName
							),
							e
					);
				}
			}
		}

		return ctClassList;
	}

	private void collectCtClasses(List<CtClass> ctClassList, CtClass ctClass) {
		if ( ctClass.hasAnnotation( Entity.class ) ) {
			ctClassList.add( ctClass );
		}

		try {
			for ( CtClass nestedCtClass : ctClass.getNestedClasses() ) {
				collectCtClasses( ctClassList, nestedCtClass );
			}
		}
		catch (NotFoundException ignore) {
		}
	}
}
