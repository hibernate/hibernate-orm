package org.hibernate.jpamodelgen.test.namedquery;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.Hql;

import java.util.List;

public interface Dao {
    @Hql("from Book where title like ?1")
    TypedQuery<Book> findByTitle(String title);

    @Hql("from Book where title like ?1 order by title fetch first ?2 rows only")
    List<Book> findFirstNByTitle(String title, int N);

    @Hql("from Book where isbn = :isbn")
    Book findByIsbn(String isbn);
}
