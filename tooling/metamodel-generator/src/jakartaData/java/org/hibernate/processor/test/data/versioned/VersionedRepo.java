package org.hibernate.processor.test.data.versioned;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface VersionedRepo {
	@Query("where id(this) = ?1")
	Versioned forId(long id);

	@Query("where id(this) = ?1 and version(this) = ?2")
	Versioned forIdAndVersion(long id, int version);

	@Query("select count(this) from Versioned")
	long count();

}
