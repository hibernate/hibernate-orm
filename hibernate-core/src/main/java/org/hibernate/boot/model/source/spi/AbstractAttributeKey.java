/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeKey {
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

	public int getDepth() {
		return depth;
	}

	protected abstract char getDelimiter();

	public abstract AbstractAttributeKey append(String property);

	public AbstractAttributeKey getParent() {
		return parent;
	}

	public String getProperty() {
		return property;
	}

	public String getFullPath() {
		return fullPath;
	}

	public boolean isRoot() {
		return parent == null;
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
