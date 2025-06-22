/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.spi.DotIdentifierSequence;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * A compound path which represents a {@link org.hibernate.metamodel.mapping.ModelPart}
 * and uniquely identifies it with the runtime metamodel.
 * <p/>
 * The {@linkplain #isRoot root} will name either an
 * {@linkplain org.hibernate.metamodel.MappingMetamodel#getEntityDescriptor entity} or
 * {@linkplain org.hibernate.metamodel.MappingMetamodel#getCollectionDescriptor collection}.
 *
 * @apiNote This is an incubating SPI type, and will move to {@link org.hibernate.spi}.
 * It might be renamed to {@code org.hibernate.metamodel.model.mapping.MappingRole};
 * the term "navigable" here is meant to indicate that we could navigate to the specific
 * {@link org.hibernate.metamodel.mapping.ModelPart} given the role.
 *
 * @author Steve Ebersole
 */
@Incubating
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
		this.fullPath = fullPath( parent, localName, separator );
	}

	private String fullPath(NavigableRole parent, String localName, char separator) {
		// the _identifierMapper is a "hidden" property on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( localName ) ) {
			return parent == null ? "" : parent.getFullPath();
		}
		else {
			final String prefix;
			if ( parent != null ) {
				final String resolvedParent = parent.getFullPath();
				prefix = isEmpty( resolvedParent ) ? "" : resolvedParent + separator;
			}
			else {
				prefix = "";
			}
			return prefix + localName;
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
	 * Uses {@code #} as the separator rather than a period,
	 * the intention being that the incoming name is a
	 * {@link org.hibernate.metamodel.mapping.ModelPartContainer}
	 * of some sort.
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
	public boolean equals(final Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof NavigableRole that) ) {
			return false;
		}
		else {
			return Objects.equals( this.fullPath,  that.fullPath );
		}
	}

	@Override
	public int hashCode() {
		return fullPath.hashCode();
	}

}
