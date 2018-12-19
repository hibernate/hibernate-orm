/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.DotIdentifierSequence;
import org.hibernate.internal.util.StringHelper;

/**
 * A representation of the static "Navigable" path relative to some "root entity".
 *
 * @see org.hibernate.query.NavigablePath
 *
 * @author Steve Ebersole
 */
public class NavigableRole implements DotIdentifierSequence, Serializable {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";

	private final NavigableRole parent;
	private final String localName;
	private final String fullPath;

	public NavigableRole(NavigableRole parent, String localName) {
		this.parent = parent;
		this.localName = localName;

		// the _identifierMapper is a "hidden" property on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( localName ) ) {
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

			this.fullPath = prefix + localName;
		}
	}

	public NavigableRole(String localName) {
		this( null, localName );
	}

	public NavigableRole() {
		this( "" );
	}

	@Override
	public NavigableRole append(String property) {
		return new NavigableRole( this, property );
	}

	@Override
	public NavigableRole getParent() {
		return parent;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	public String getNavigableName() {
		return getLocalName();
	}

	@Override
	public String getFullPath() {
		return fullPath;
	}

	public boolean isRoot() {
//		return parent == null && StringHelper.isEmpty( navigableName );
		return parent == null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + fullPath + ']';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		NavigableRole that = (NavigableRole) o;
		return Objects.equals( getFullPath(), that.getFullPath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( getFullPath() );
	}
}
