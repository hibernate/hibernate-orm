package org.hibernate.processor.test.data.wildcard;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.StatelessSession;

import java.util.List;

@Repository
public interface WildcardRepository extends ManagedOperations<MyEntity> {
	StatelessSession session();

	@Query("from MyWildcardEntity")
	List<MyEntity> findAll();
}
