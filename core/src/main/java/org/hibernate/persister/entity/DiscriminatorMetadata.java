/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.persister.entity;

import org.hibernate.type.Type;

/**
 * Provides the information needed to properly handle type discrimination
 * in HQL queries, either by 'something.class' or 'type(something)' references.
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorMetadata {
	/**
	 * Get the sql fragment that is used to determine the actual discriminator value for a row.
	 *
	 * @param sqlQualificationAlias The qualification alias to append to any columns references in
	 * the generated fragment.
	 *
	 * @return The fragment
	 */
	public String getSqlFragment(String sqlQualificationAlias);

	/**
	 * Get the type used to resolve the actual discriminator value resulting from
	 * {@link #getSqlFragment} back into a {@link Class} reference.
	 *
	 * @return The resolution type.
	 */
	public Type getResolutionType();
}
