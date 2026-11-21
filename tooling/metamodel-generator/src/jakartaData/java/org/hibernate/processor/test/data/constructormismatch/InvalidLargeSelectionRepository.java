package org.hibernate.processor.test.data.constructormismatch;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface InvalidLargeSelectionRepository {
	@Query("select 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', "
			+ "'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p' from Book")
	List<Summary> summaries();
}
