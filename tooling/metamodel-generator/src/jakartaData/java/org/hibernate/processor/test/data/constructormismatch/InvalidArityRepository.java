package org.hibernate.processor.test.data.constructormismatch;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface InvalidArityRepository {
	@Query("select 'zzz', 1, id from Book")
	List<OverloadedSummary> summaries();
}
