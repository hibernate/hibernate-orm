/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.versioned;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface SpecialVersionedRepo {
	@Query("where id(this) = ?1")
	SpecialVersioned forId(long id);

	@Query("where id(this) = ?1 and version(this) = ?2")
	SpecialVersioned forIdAndVersion(long id, int version);

	@Query("select count(this) from SpecialVersioned")
	long count();

}
