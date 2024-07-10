/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.spi;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.internal.util.StringHelper;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

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
		this.alias = alias = StringHelper.nullIfEmpty( alias );
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
		this.alias = alias = StringHelper.nullIfEmpty( alias );

		final String aliasedLocalName = alias == null
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
			this.identifierForTableGroup = StringHelper.isEmpty( parentFullPath )
					? aliasedLocalName
					: parentFullPath + "." + localName;
		}
	}

	/**
	 * @deprecated Since {@link FullPathCalculator} is no longer used
	 */
	@Deprecated( since = "6.6", forRemoval = true )
	public NavigablePath(
			@Nullable NavigablePath parent,
			String localName,
			@Nullable String alias,
			String identifierForTableGroup,
			FullPathCalculator fullPathCalculator,
			int hashCode) {
		this( parent, localName, alias, identifierForTableGroup, hashCode );
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
		this.alias = StringHelper.nullIfEmpty( alias );
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

		if ( other == null ) {
			return false;
		}

		final DotIdentifierSequence otherPath = (DotIdentifierSequence) other;
		if ( ! localNamesMatch( otherPath ) ) {
			return false;
		}

		if ( otherPath instanceof NavigablePath ) {
			final NavigablePath otherNavigablePath = (NavigablePath) otherPath;
			if ( ! Objects.equals( getAlias(), otherNavigablePath.getAlias() ) ) {
				return false;
			}
			return Objects.equals( getRealParent(), otherNavigablePath.getRealParent() );
		}

		return Objects.equals( getParent(), otherPath.getParent() );
	}

	protected boolean localNamesMatch(DotIdentifierSequence other) {
		if ( other instanceof EntityIdentifierNavigablePath ) {
			return localNamesMatch( (EntityIdentifierNavigablePath) other );
		}

		return Objects.equals( getLocalName(), other.getLocalName() );
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
		if ( !localNamesMatch( dotIdentifierSequence ) ) {
			return false;
		}
		NavigablePath parent = getParent();
		return parent != null && parent.isSuffix( dotIdentifierSequence.getParent() );
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
		if ( !getLocalName().equals( suffix.getLocalName() ) ) {
			return null;
		}
		NavigablePath parent = getParent();
		if ( parent != null ) {
			return parent.trimSuffix( suffix.getParent() );
		}
		return null;
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

	public boolean pathsMatch(@Nullable NavigablePath p) {
		return this == p || p != null && localName.equals( p.localName )
				&& ( parent == null ? p.parent == null && Objects.equals( alias, p.alias ) : parent.pathsMatch( p.parent ) );
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

		final RelativePathCollector pathCollector = new RelativePathCollector();
		relativize( base, pathCollector );
		return pathCollector.resolve();
	}

	protected static class RelativePathCollector {
		private boolean matchedBase;
		private StringBuilder buffer;

		public void collectPath(String path) {
			if ( !matchedBase ) {
				return;
			}

			if ( buffer == null ) {
				buffer = new StringBuilder();
			}
			else {
				buffer.append( '.' );
			}

			buffer.append( path );
		}

		public @Nullable String resolve() {
			if ( buffer == null ) {
				// Return an empty string instead of null in case the two navigable paths are equal
				return matchedBase ? "" : null;
			}
			return buffer.toString();
		}
	}

	protected void relativize(NavigablePath base, RelativePathCollector collector) {
		if ( this.equals( base ) ) {
			collector.matchedBase = true;
			return;
		}

		if ( ! collector.matchedBase ) {
			if ( parent != null ) {
				parent.relativize( base, collector );
			}
		}

		collector.collectPath( getLocalName() );
	}

	@Override
	public String getFullPath() {
		return alias == null ? identifierForTableGroup : identifierForTableGroup + "(" + alias + ")";
	}

	@Override
	public String toString() {
		return getFullPath();
	}

	/**
	 * Effectively a tri-function
	 *
	 * @deprecated No longer used
	 */
	@FunctionalInterface
	@Deprecated( since = "6.6", forRemoval = true )
	protected interface FullPathCalculator extends Serializable {
		String calculateFullPath(@Nullable NavigablePath parent, String localName, @Nullable String alias);
	}

	/**
	 * The pattern used for root NavigablePaths
	 *
	 * @deprecated No longer used
	 */
	@Deprecated( since = "6.6", forRemoval = true )
	protected static String calculateRootFullPath(@Nullable NavigablePath parent, String rootName, @Nullable String alias) {
		assert parent == null;
		return alias == null ? rootName : rootName + "(" + alias + ")";
	}

	/**
	 * The normal pattern used for the "full path"
	 *
	 * @deprecated No longer used
	 */
	@Deprecated( since = "6.6", forRemoval = true )
	private static String calculateNormalFullPath(@Nullable NavigablePath parent, String localName, @Nullable String alias) {
		final String parentFullPath = castNonNull( parent ).getFullPath();
		final String baseFullPath = StringHelper.isEmpty( parentFullPath )
				? localName
				: parentFullPath + "." + localName;
		return alias == null ? baseFullPath : baseFullPath + "(" + alias + ")";
	}

	/**
	 * Pattern used for `_identifierMapper`
	 *
	 * @deprecated No longer used
	 */
	@Deprecated( since = "6.6", forRemoval = true )
	protected static String calculateIdMapperFullPath(@Nullable NavigablePath parent, String localName, @Nullable String alias) {
		return parent != null ? parent.getFullPath() : "";
	}
}
