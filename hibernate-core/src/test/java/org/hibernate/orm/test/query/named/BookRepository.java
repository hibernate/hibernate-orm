package org.hibernate.orm.test.query.named;

import jakarta.data.repository.Repository;

@Repository
public interface BookRepository
		extends BookRepositoryQueries,
				GenericBookRepository<Book> {
}
