/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
