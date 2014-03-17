/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.spi;

/**
 * Describe the source of filer def information. Generally either {@code <filter-def/>} or
 * {@link org.hibernate.annotations.FilterDef @FilterDef}
 *
 * @author Steve Ebersole
 */
public interface FilterDefinitionSource {
	/**
	 * Retrieve the name of the filter.  Would match the related {@link FilterSource#getName}
	 *
	 * @return The filter name
	 *
	 * @see FilterSource#getName
	 */
	public String getName();

	/**
	 * Retrieve the condition specified as part of the def.  Defines the condition to use
	 * in related filters when {@link FilterSource#getCondition} is null.
	 *
	 * @return The "default" condition for associated filters.
	 *
	 * @see FilterSource#getCondition
	 */
	public String getCondition();

	/**
	 * Retrieve parameter sources associated with this filer def.
	 *
	 * @return The parameter sources. Can be null.
	 */
	public Iterable<FilterParameterSource> getParameterSources();
}
