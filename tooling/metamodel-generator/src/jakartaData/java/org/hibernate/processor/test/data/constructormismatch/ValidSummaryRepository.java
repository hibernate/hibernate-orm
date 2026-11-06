package org.hibernate.processor.test.data.constructormismatch;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface ValidSummaryRepository {
	@Query("select 'zzz', 1 from Book")
	List<Summary> summaries();
}
