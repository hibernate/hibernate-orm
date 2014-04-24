/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi;

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
