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
package org.hibernate.hql.internal.ast;
import org.hibernate.type.Type;

/**
 * Essentially a wrapper around a {@link org.hibernate.persister.entity.DiscriminatorMetadata}
 * and the proper sql alias to use.
 *
 * @author Steve Ebersole
 */
public interface TypeDiscriminatorMetadata {
	/**
	 * Get the sql fragment that is used to determine the actual discriminator value for a row.
	 *
	 * @return The fragment
	 */
	public String getSqlFragment();

	/**
	 * Get the type used to resolve the actual discriminator value resulting from
	 * {@link #getSqlFragment} back into a {@link Class} reference.
	 *
	 * @return The resolution type.
	 */
	public Type getResolutionType();
}
