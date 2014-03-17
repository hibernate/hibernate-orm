/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PropertyPath {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";

	private final char delimiter;
	private final PropertyPath parent;
	private final String property;
	private final String fullPath;

	public PropertyPath() {
		this( null, "", '.' );
	}

	public PropertyPath(char delimiter) {
		this( null, "", delimiter );
	}

	public PropertyPath(String base) {
		this( null, base, '.' );
	}

	public PropertyPath(String base, char delimiter) {
		this( null, base, delimiter );
	}

	private PropertyPath(PropertyPath parent, String property) {
		this( parent, property, parent.delimiter );
	}

	private PropertyPath(PropertyPath parent, String property, char delimiter) {
		this.parent = parent;
		this.property = property;
		this.delimiter = delimiter;

		// the _identifierMapper is a "hidden" property on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( property ) ) {
			this.fullPath = parent != null ? parent.getFullPath() : "";
		}
		else {
			final String prefix;
			if ( parent != null ) {
				final String resolvedParent = parent.getFullPath();
				if ( StringHelper.isEmpty( resolvedParent ) ) {
					prefix = "";
				}
				else {
					prefix = resolvedParent + delimiter;
				}
			}
			else {
				prefix = "";
			}

			this.fullPath = prefix + property;
		}
	}

	public PropertyPath append(String property) {
		return new PropertyPath( this, property );
	}

	public PropertyPath getParent() {
		return parent;
	}

	public String getProperty() {
		return property;
	}

	public String getFullPath() {
		return fullPath;
	}

	public char getDelimiter() {
		return delimiter;
	}

	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + fullPath + ']';
	}
}
