/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.metamodel.source.spi;

import org.hibernate.xml.spi.Origin;

/**
 * @author Brett Meyer
 */
public class InvalidMappingException extends org.hibernate.InvalidMappingException {

	private final Origin origin;

	public InvalidMappingException(Origin origin) {
		super(
				String.format( "Could not parse mapping document: %s (%s)", origin.getName(), origin.getType() ),
				origin
		);
		this.origin = origin;
	}

	public InvalidMappingException(Origin origin, Throwable e) {
		super(
				String.format( "Could not parse mapping document: %s (%s)", origin.getName(), origin.getType() ),
				origin.getType().name(),
				origin.getName(),
				e
		);
		this.origin = origin;
	}

	public Origin getOrigin() {
		return origin;
	}
}
