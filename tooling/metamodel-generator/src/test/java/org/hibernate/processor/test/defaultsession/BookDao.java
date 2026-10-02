package org.hibernate.processor.test.defaultsession;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.Find;

import java.util.List;

public interface BookDao {

	/** A default session getter must be used as-is: no injected session, no override. */
	default EntityManager entityManager() {
		return null;
	}

	@Find
	List<Book> findByTitle(String title);
}
