package org.hibernate.processor.test.data.stateful;

import jakarta.data.repository.Repository;
import jakarta.data.repository.stateful.Persist;
import org.hibernate.StatelessSession;

@Repository
public interface InvalidStatelessBackedStatefulBookRepository {
	StatelessSession session();

	@Persist
	void persist(StatefulBook book);
}
