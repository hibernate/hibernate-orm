package org.hibernate.processor.test.data.constructormismatch;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface InvalidSummaryRepository {
	@Query("""
			select 'zzz', array_agg(distinct cast(id as int)) over ()
			from Book
			group by id
			""")
	List<Summary> summaries();
}
