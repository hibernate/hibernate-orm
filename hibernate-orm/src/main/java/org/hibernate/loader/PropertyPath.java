/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PropertyPath {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";
	private final PropertyPath parent;
	private final String property;
	private final String fullPath;

	public PropertyPath(PropertyPath parent, String property) {
		this.parent = parent;
		this.property = property;

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
					prefix = resolvedParent + '.';
				}
			}
			else {
				prefix = "";
			}

			this.fullPath = prefix + property;
		}
	}

	public PropertyPath(String property) {
		this( null, property );
	}

	public PropertyPath() {
		this( "" );
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

	public boolean isRoot() {
		return parent == null && StringHelper.isEmpty( property );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + fullPath + ']';
	}
}
