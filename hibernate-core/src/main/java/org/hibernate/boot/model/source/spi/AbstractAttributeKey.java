/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeKey {
	// todo : replace this with "{element}"
	private static final String COLLECTION_ELEMENT = "collection&&element";
	private static final String DOT_COLLECTION_ELEMENT = '.' + COLLECTION_ELEMENT;
	private static final Pattern DOT_COLLECTION_ELEMENT_PATTERN = Pattern.compile(
			DOT_COLLECTION_ELEMENT,
			Pattern.LITERAL
	);

	private final AbstractAttributeKey parent;
	private final String property;
	private final String fullPath;
	private final int depth;

	/**
	 * Constructor for the base AttributePath
	 */
	protected AbstractAttributeKey() {
		this( null, "" );
	}

	/**
	 * Constructor for the base AttributeRole
	 */
	protected AbstractAttributeKey(String base) {
		this( null, base );
	}

	protected AbstractAttributeKey(AbstractAttributeKey parent, String property) {
		this.parent = parent;
		this.property = property;

		final String prefix;
		if ( parent != null ) {
			final String resolvedParent = parent.getFullPath();
			if ( StringHelper.isEmpty( resolvedParent ) ) {
				prefix = "";
			}
			else {
				prefix = resolvedParent + getDelimiter();
			}
			depth = parent.getDepth() + 1;
		}
		else {
			prefix = "";
			depth = 0;
		}

		this.fullPath = prefix + property;
	}

	/**
	 * How many "parts" are there to this path/role?
	 *
	 * @return The number of parts.
	 */
	public int getDepth() {
		return depth;
	}

	protected abstract char getDelimiter();

	/**
	 * Creates a new AbstractAttributeKey by appending the passed part.
	 *
	 * @param property The part to append
	 *
	 * @return The new AbstractAttributeKey
	 */
	public abstract AbstractAttributeKey append(String property);

	/**
	 * Access to the parent part
	 *
	 * @return the parent part
	 */
	public AbstractAttributeKey getParent() {
		return parent;
	}

	/**
	 * Access to the end path part.
	 *
	 * @return the end path part
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Access to the full path as a String
	 *
	 * @return The full path as a String
	 */
	public String getFullPath() {
		return fullPath;
	}

	/**
	 * Does this part represent a root.
	 *
	 * @return {@code true} if this part is a root.
	 */
	public boolean isRoot() {
		return parent == null;
	}

	/**
	 * Does this part represent a collection-element reference?
	 *
	 * @return {@code true} if the current property is a collection element
	 * marker ({@link #COLLECTION_ELEMENT}
	 */
	public boolean isCollectionElement() {
		return COLLECTION_ELEMENT.equals( property );
	}

	/**
	 * Does any part represent a collection-element reference?
	 *
	 * @return {@code true} if this part or any parent part is a collection element
	 * marker ({@link #COLLECTION_ELEMENT}.
	 */
	public boolean isPartOfCollectionElement() {
		return fullPath.contains( DOT_COLLECTION_ELEMENT );
	}

	public String stripCollectionElementMarker() {
		return DOT_COLLECTION_ELEMENT_PATTERN.matcher( fullPath ).replaceAll( Matcher.quoteReplacement( "" ) );
	}

	@Override
	public String toString() {
		return getFullPath();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AbstractAttributeKey that = (AbstractAttributeKey) o;
		return this.fullPath.equals( that.fullPath );

	}

	@Override
	public int hashCode() {
		return fullPath.hashCode();
	}
}
