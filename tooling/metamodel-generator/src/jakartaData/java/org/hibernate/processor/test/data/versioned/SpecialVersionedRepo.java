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
