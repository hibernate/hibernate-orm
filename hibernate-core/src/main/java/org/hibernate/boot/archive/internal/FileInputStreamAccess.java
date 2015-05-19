/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * An InputStreamAccess implementation based on a File reference
 *
 * @author Steve Ebersole
 */
public class FileInputStreamAccess implements InputStreamAccess {
	private final String name;
	private final File file;

	public FileInputStreamAccess(String name, File file) {
		this.name = name;
		this.file = file;
		if ( ! file.exists() ) {
			throw new HibernateException( "File must exist : " + file.getAbsolutePath() );
		}
	}

	@Override
	public String getStreamName() {
		return name;
	}

	@Override
	public InputStream accessInputStream() {
		try {
			return new BufferedInputStream( new FileInputStream( file ) );
		}
		catch (FileNotFoundException e) {
			// should never ever ever happen, but...
			throw new ArchiveException(
					"File believed to exist based on File.exists threw error when passed to FileInputStream ctor",
					e
			);
		}
	}
}
