/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spi;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.Incubating;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * A compound name where the root path element is an entity name or a collection role
 * and each the path sub-path from the root references a domain or mapping model part
 * relative to a root path.
 *
 * @author Steve Ebersole
 */
@Incubating
public class NavigablePath implements DotIdentifierSequence, Serializable {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";

	private final @Nullable NavigablePath parent;
	private final String localName;
	private final @Nullable String alias;
	private final String identifierForTableGroup;
	private final int hashCode;

	public NavigablePath(String localName) {
		this( localName, null );
	}

	public NavigablePath(String rootName, @Nullable String alias) {
		this.parent = null;
		this.alias = alias = nullIfEmpty( alias );
		this.localName = rootName;
		this.identifierForTableGroup = rootName;
		this.hashCode = localName.hashCode() + ( alias == null ? 0 : alias.hashCode() );
	}

	public NavigablePath(NavigablePath parent, String navigableName) {
		this( parent, navigableName, null );
	}

	public NavigablePath(NavigablePath parent, String localName, @Nullable String alias) {
		assert parent != null;
		this.parent = parent;
		this.alias = alias = nullIfEmpty( alias );
		final String aliasedLocalName =
				alias == null
						? localName
						: localName + '(' + alias + ')';
		this.hashCode = parent.hashCode() + aliasedLocalName.hashCode();

		// the _identifierMapper is a "hidden property" on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( localName ) ) {
			this.localName = "";
			this.identifierForTableGroup = parent.getFullPath();
		}
		else {
			this.localName = localName;
			final String parentFullPath = parent.getFullPath();
			this.identifierForTableGroup =
					isEmpty( parentFullPath )
							? aliasedLocalName
							: parentFullPath + "." + localName;
		}
	}

	public NavigablePath(
			@Nullable NavigablePath parent,
			String localName,
			@Nullable String alias,
			String identifierForTableGroup,
			int hashCode) {
		this.parent = parent;
		this.localName = localName;
		this.hashCode = hashCode;
		this.alias = nullIfEmpty( alias );
		this.identifierForTableGroup = identifierForTableGroup;
	}

	@Override
	public @Nullable NavigablePath getParent() {
		return parent instanceof TreatedNavigablePath ? parent.getParent() : parent;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	public @Nullable String getAlias() {
		return alias;
	}

	public boolean isAliased() {
		return alias != null;
	}

	public String getIdentifierForTableGroup() {
		return identifierForTableGroup;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if ( this == other ) {
			return true;
		}
		else if ( !(other instanceof DotIdentifierSequence otherPath) ) {
			return false;
		}
		else {
			if ( !localNamesMatch( otherPath ) ) {
				return false;
			}
			else if ( otherPath instanceof NavigablePath otherNavigablePath ) {
				return Objects.equals( getAlias(), otherNavigablePath.getAlias() )
					&& Objects.equals( getRealParent(), otherNavigablePath.getRealParent() );
			}
			else {
				return Objects.equals( getParent(), otherPath.getParent() );
			}
		}
	}

	protected boolean localNamesMatch(DotIdentifierSequence other) {
		return other instanceof EntityIdentifierNavigablePath entityIdentifierNavigablePath
				? localNamesMatch( entityIdentifierNavigablePath )
				: Objects.equals( getLocalName(), other.getLocalName() );
	}

	protected boolean localNamesMatch(EntityIdentifierNavigablePath other) {
		return Objects.equals( getLocalName(), other.getLocalName() )
			|| Objects.equals( getLocalName(), other.getIdentifierAttributeName() );
	}

	public NavigablePath append(String property) {
		return new NavigablePath( this, property );
	}

	public NavigablePath append(String property, String alias) {
		return new NavigablePath( this, property, alias );
	}

	public NavigablePath treatAs(String entityName) {
		return new TreatedNavigablePath( this, entityName );
	}

	public NavigablePath treatAs(String entityName, String alias) {
		return new TreatedNavigablePath( this, entityName, alias );
	}

	public @Nullable NavigablePath getRealParent() {
		return parent;
	}

	/**
	 * Determine whether this path is part of the given path's parent
	 */
	public boolean isParent(@Nullable NavigablePath navigablePath) {
		while ( navigablePath != null ) {
			if ( this.equals( navigablePath.getParent() ) ) {
				return true;
			}
			navigablePath = navigablePath.getParent();
		}
		return false;
	}

	/**
	 * Determine whether the given path is a suffix of this path
	 */
	public boolean isSuffix(@Nullable DotIdentifierSequence dotIdentifierSequence) {
		if ( dotIdentifierSequence == null ) {
			return true;
		}
		else if ( !localNamesMatch( dotIdentifierSequence ) ) {
			return false;
		}
		else {
			final NavigablePath parent = getParent();
			return parent != null
				&& parent.isSuffix( dotIdentifierSequence.getParent() );
		}
	}

	/**
	 *
	 * Removes the suffix part from the NavigablePath,
	 * when the NavigablePath does not contain the suffix it returns null;
	 *
	 * @param suffix the part to remove from the NavigablePath
	 *
	 * @return the NavigablePath stripped of the suffix part
	 * or null if the NavigablePath does not contain the suffix.
	 *
	 */
	public @Nullable NavigablePath trimSuffix(@Nullable DotIdentifierSequence suffix) {
		if ( suffix == null ) {
			return this;
		}
		else if ( !getLocalName().equals( suffix.getLocalName() ) ) {
			return null;
		}
		else {
			final NavigablePath parent = getParent();
			return parent != null
					? parent.trimSuffix( suffix.getParent() )
					: null;
		}
	}

	/**
	 * Determine whether this path is part of the given path's parent
	 */
	public boolean isParentOrEqual(@Nullable NavigablePath navigablePath) {
		while ( navigablePath != null ) {
			if ( this.equals( navigablePath ) ) {
				return true;
			}
			navigablePath = navigablePath.getParent();
		}
		return false;
	}

	public boolean pathsMatch(@Nullable NavigablePath path) {
		return this == path
			|| path != null && localName.equals( path.localName ) && (
				parent == null
						? path.parent == null && Objects.equals( alias, path.alias )
						: parent.pathsMatch( path.parent )
			);
	}

	/**
	 * Ignores aliases in the resulting String
	 */
	public @Nullable String relativize(NavigablePath base) {
		// e.g.
		//	- base = Root.sub
		//	- this = Root.sub.stuff
		//	- result = stuff
		// e.g. 2
		//	- base = Root.sub
		//	- this = Root.sub.leaf.terminal
		//	- result = leaf.terminal

		final var pathCollector = new RelativePathCollector();
		relativize( base, pathCollector );
		return pathCollector.resolve();
	}

	protected static class RelativePathCollector {
		private boolean matchedBase;
		private StringBuilder buffer;

		public void collectPath(String path) {
			if ( matchedBase ) {
				if ( buffer == null ) {
					buffer = new StringBuilder();
				}
				else {
					buffer.append( '.' );
				}

				buffer.append( path );
			}
		}

		public @Nullable String resolve() {
			if ( buffer == null ) {
				// Return an empty string instead of null in case the two navigable paths are equal
				return matchedBase ? "" : null;
			}
			else {
				return buffer.toString();
			}
		}
	}

	protected void relativize(NavigablePath base, RelativePathCollector collector) {
		if ( this.equals( base ) ) {
			collector.matchedBase = true;
		}
		else {
			if ( !collector.matchedBase ) {
				if ( parent != null ) {
					parent.relativize( base, collector );
				}
			}
			collector.collectPath( getLocalName() );
		}
	}

	@Override
	public String getFullPath() {
		return alias == null
				? identifierForTableGroup
				: identifierForTableGroup + "(" + alias + ")";
	}

	@Override
	public String toString() {
		return getFullPath();
	}
}
