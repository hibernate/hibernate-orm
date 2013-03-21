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
package org.hibernate.jpa.boot.spi;

import java.io.InputStream;

/**
 * Bundles together a stream and the name that was used to locate it.  The name is often useful for logging.
 *
 * @deprecated Use {@link org.hibernate.jpa.boot.spi.InputStreamAccess} instead.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Deprecated
public class NamedInputStream {
	private final String name;
	private final InputStream stream;

	public NamedInputStream(String name, InputStream stream) {
		this.name = name;
		this.stream = stream;
	}

	public InputStream getStream() {
		return stream;
	}

	public String getName() {
		return name;
	}
}
