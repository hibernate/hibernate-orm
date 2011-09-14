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
package org.hibernate.internal.jaxb;

import java.io.Serializable;

/**
 * Describes the origin of an xml document
 *
 * @author Steve Ebersole
 */
public class Origin implements Serializable {
	private final SourceType type;
	private final String name;

	public Origin(SourceType type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * Retrieve the type of origin.
	 *
	 * @return The origin type.
	 */
	public SourceType getType() {
		return type;
	}

	/**
	 * The name of the document origin.  Interpretation is relative to the type, but might be the
	 * resource name or file URL.
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}
}
