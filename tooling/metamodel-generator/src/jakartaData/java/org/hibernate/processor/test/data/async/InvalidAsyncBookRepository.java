package org.hibernate.processor.test.data.async;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

@Repository
public interface InvalidAsyncBookRepository {
	@Asynchronous
	@Find
	AsyncBook bookByIsbn(String isbn);
}
