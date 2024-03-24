package org.hibernate.processor.test.hqlvalidation;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import java.util.List;

public interface Dao2 {
    @HQL("from Book where tile like ?1")
    TypedQuery<Book> findByTitle(String title);

    @HQL("from Book where title like ?1 order by title fetch first ?2 rows only")
    List<Book> findFirstNByTitle(String title, int N);

    @HQL("from Book where isbn = :isbn")
    Book findByIsbn(String isbn);

    @SQL("select * from Book where isbn = :isbn")
    Book findByIsbnNative(String isbn);
}
