package org.hibernate.processor.test.data.stateful;

import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

@Repository
public interface StatelessBookRepository {
	@Save
	void save(StatefulBook book);
}
