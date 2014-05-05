/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.archive.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.archive.spi.ArchiveException;
import org.hibernate.metamodel.archive.spi.InputStreamAccess;

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
