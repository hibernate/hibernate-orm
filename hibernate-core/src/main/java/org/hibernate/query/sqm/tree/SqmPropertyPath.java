/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Essentially a copy of ORM's org.hibernate.loader.PropertyPath
 *
 * @author Steve Ebersole
 */
public class SqmPropertyPath {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";
	private final SqmPropertyPath parent;
	private final String localPath;
	private final String fullPath;

	public SqmPropertyPath(SqmPropertyPath parent, String localPath) {
		this.parent = parent;
		this.localPath = localPath;

		// the _identifierMapper is a "hidden" property on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( localPath ) ) {
			this.fullPath = parent != null ? parent.getFullPath() : "";
		}
		else {
			final String prefix;
			if ( parent != null ) {
				final String resolvedParent = parent.getFullPath();
				if ( isEmpty( resolvedParent ) ) {
					prefix = "";
				}
				else {
					prefix = resolvedParent + '.';
				}
			}
			else {
				prefix = "";
			}

			this.fullPath = prefix + localPath;
		}
	}

	public SqmPropertyPath append(String property) {
		return new SqmPropertyPath( this, property );
	}

	public SqmPropertyPath getParent() {
		return parent;
	}

	public String getLocalPath() {
		return localPath;
	}

	public String getFullPath() {
		return fullPath;
	}

	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + fullPath + ']';
	}
}
