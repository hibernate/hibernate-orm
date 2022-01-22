/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

/**
 * Encapsulates and manages a Jandex Index
 *
 * @author Steve Ebersole
 */
public class IndexManager {
	private final Provider<Directory> classesDirectoryReferenceAccess;
	private final Provider<RegularFile> indexFileReferenceAccess;
	private final Project project;

	private Index index;

	public IndexManager(
			Provider<Directory> classesDirectoryReferenceAccess,
			Project project) {
		this.classesDirectoryReferenceAccess = classesDirectoryReferenceAccess;
		this.indexFileReferenceAccess = project.getLayout()
				.getBuildDirectory()
				.file( "post/" + project.getName() + ".idx" );
		this.project = project;
	}

	public Provider<Directory> getClassesDirectoryReferenceAccess() {
		return classesDirectoryReferenceAccess;
	}

	public Provider<RegularFile> getIndexFileReferenceAccess() {
		return indexFileReferenceAccess;
	}

	public Index getIndex() {
		if ( index == null ) {
			throw new IllegalStateException( "Index has not been created yet" );
		}
		return index;
	}

	void index() {
		if ( index != null ) {
			return;
		}

		final Indexer indexer = new Indexer();

		final Directory classesDirectory = classesDirectoryReferenceAccess.get();
		final Set<File> classFiles = classesDirectory.getAsFileTree().getFiles();
		for ( File classFile : classFiles ) {
			if ( !classFile.getName().endsWith( ".class" ) ) {
				continue;
			}

			if ( !classFile.getAbsolutePath().contains( "org/hibernate" ) ) {
				continue;
			}

			try ( final FileInputStream stream = new FileInputStream( classFile ) ) {
				final ClassInfo indexedClassInfo = indexer.index( stream );
				if ( indexedClassInfo == null ) {
					project.getLogger().lifecycle( "Problem indexing class file - " + classFile.getAbsolutePath() );
				}
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException( "Problem locating project class file - " + classFile.getAbsolutePath(), e );
			}
			catch (IOException e) {
				throw new RuntimeException( "Error accessing project class file - " + classFile.getAbsolutePath(), e );
			}
		}

		this.index = indexer.complete();
		storeIndex( index );
	}

	private void storeIndex(Index index) {
		final File indexFile = indexFileReferenceAccess.get().getAsFile();
		if ( indexFile.exists() ) {
			indexFile.delete();
		}

		try {
			indexFile.getParentFile().mkdirs();
			indexFile.createNewFile();
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create index file - " + indexFile.getAbsolutePath(), e );
		}

		try ( final FileOutputStream stream = new FileOutputStream( indexFile ) ) {
			final IndexWriter indexWriter = new IndexWriter( stream );
			indexWriter.write( index );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen", e );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error accessing index file - " + indexFile.getAbsolutePath(), e );
		}
	}
}
