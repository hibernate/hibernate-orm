/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
