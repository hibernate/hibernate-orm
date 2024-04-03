/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.io.Serializable;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.spi.DotIdentifierSequence;
import org.hibernate.spi.NavigablePath;

/**
 * A compound path which represents a {@link org.hibernate.metamodel.mapping.ModelPart}
 * and uniquely identifies it with the runtime metamodel.
 * <p/>
 * The {@linkplain #isRoot() root} will name either an
 * {@linkplain org.hibernate.metamodel.MappingMetamodel#getEntityDescriptor entity} or
 * {@linkplain org.hibernate.metamodel.MappingMetamodel#getCollectionDescriptor collection}
 *
 *
 * @apiNote Poorly named. Should probably have been `org.hibernate.metamodel.model.mapping.MappingRole`;
 * the term "navigable" here is meant to indicate that we could navigate to the specific
 * {@link org.hibernate.metamodel.mapping.ModelPart} given the role.
 *
 * @author Steve Ebersole
 */
public final class NavigableRole implements DotIdentifierSequence, Serializable {
	public static final String IDENTIFIER_MAPPER_PROPERTY = NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

	private final NavigableRole parent;
	private final String localName;
	private final String fullPath;

	public NavigableRole(NavigableRole parent, String localName) {
		this( parent, localName, '.' );
	}

	public NavigableRole(NavigableRole parent, String localName, char separator) {
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
					prefix = resolvedParent + separator;
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

	public NavigableRole append(String name) {
		return new NavigableRole( this, name );
	}

	/**
	 * Uses `#` as the separator rather than `.`.  The intention being that the incoming name is a
	 * {@link org.hibernate.metamodel.mapping.ModelPartContainer} of some sort
	 *
	 * todo (6.0) : better name?
	 */
	public NavigableRole appendContainer(String name) {
		return new NavigableRole( this, name, '#' );
	}

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

	public String getFullPath() {
		return fullPath;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + fullPath + ']';
	}

	@Override
	public boolean equals(final Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || NavigableRole.class != o.getClass() ) {
			return false;
		}
		NavigableRole that = (NavigableRole) o;
		return fullPath.equals( that.fullPath );
	}

	@Override
	public int hashCode() {
		return this.fullPath.hashCode();
	}

}
