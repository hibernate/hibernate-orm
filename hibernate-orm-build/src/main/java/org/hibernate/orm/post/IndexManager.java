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

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

/**
 * Encapsulates and manages a Jandex Index
 *
 * @author Steve Ebersole
 */
public class IndexManager {
	private final Provider<RegularFile> jarFileReference;
	private final Provider<RegularFile> indexFileReference;

	private Index index;

	public IndexManager(Provider<RegularFile> jarFileReference, Provider<RegularFile> indexFileReference) {
		this.jarFileReference = jarFileReference;
		this.indexFileReference = indexFileReference;
	}

	public Provider<RegularFile> getJarFileReference() {
		return jarFileReference;
	}

	public Provider<RegularFile> getIndexFileReference() {
		return indexFileReference;
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

		final File jarFileAsFile = jarFileReference.get().getAsFile();
		final Indexer indexer = new Indexer();
		try ( final FileInputStream stream = new FileInputStream( jarFileAsFile ) ) {
			indexer.index( stream );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Unable to locate project jar file - " + jarFileAsFile.getAbsolutePath(), e );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error accessing project jar file - " + jarFileAsFile.getAbsolutePath(), e );
		}

		this.index = indexer.complete();
		storeIndex( index );
	}

	private void storeIndex(Index index) {
		final File indexFile = indexFileReference.get().getAsFile();
		if ( indexFile.exists() ) {
			indexFile.delete();
		}

		try {
			indexFile.mkdirs();
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
