/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.Objects;

import org.hibernate.internal.util.Loggable;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * A representation of the path to a particular Navigable
 * as part of a query relative to a "navigable root".
 *
 * @see NavigableRole
 *
 * @author Steve Ebersole
 */
public class NavigablePath implements Loggable {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";

	private final NavigablePath parent;
	private final String localName;
	private final String fullPath;

	public NavigablePath(NavigablePath parent, String navigableName) {
		this.parent = parent;
		this.localName = navigableName;

		// the _identifierMapper is a "hidden property" on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( navigableName ) ) {
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

			this.fullPath = prefix + navigableName;
		}
	}

	public NavigablePath(String localName) {
		this( null, localName );
	}

	public NavigablePath() {
		this( "" );
	}

	public NavigablePath append(String property) {
		return new NavigablePath( this, property );
	}

	public NavigablePath getParent() {
		return parent;
	}

	public String getLocalName() {
		return localName;
	}

	public String getFullPath() {
		return fullPath;
	}

	public boolean isRoot() {
//		return parent == null && StringHelper.isEmpty( localName );
		return parent == null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + fullPath + ']';
	}

	@Override
	public int hashCode() {
		return fullPath.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		NavigablePath path = (NavigablePath) o;
		return Objects.equals( getFullPath(), path.getFullPath() );
	}

	@Override
	public String toLoggableFragment() {
		return getLocalName();
	}
}
