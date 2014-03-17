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
package org.hibernate.metamodel.source.internal.annotations.attribute;

/**
 * Helper for determining insertability/updateability of an attribute's columns.
 * <p/>
 * By default an attribute's columns are considered writable.  Cases which
 * indicate it is not writable should call {@link #disable}.
 * <p/>
 * Additionally inclusion can be completely disabled up front via the boolean
 * argument to the constructor to force non-inclusion from containing contexts.
 * Makes it easier to process.
 *
 * @author Steve Ebersole
 */
public class ColumnInclusion {
	private final boolean canBeIncluded;
	private boolean included = true;

	/**
	 * Creates the inclusion helper.
	 *
	 * @param canBeIncluded {@code false} here indicates that the inclusion can
	 * never be included
	 */
	public ColumnInclusion(boolean canBeIncluded) {
		this.canBeIncluded = canBeIncluded;
	}

	public void disable() {
		this.included = false;
	}

	public boolean shouldInclude() {
		return canBeIncluded && included;

	}
}
