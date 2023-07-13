package org.hibernate.jpamodelgen.test.hqlsql;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import java.util.List;

public abstract class Books {
    @Find
    abstract Book getBook(EntityManager entityManager, String isbn);

    @Find
    abstract Book getBook(StatelessSession session, String title, String isbn);

    @Find
    abstract Book getBookByNaturalKey(Session session, String authorName, String title);

    @HQL("from Book where title like ?1")
    abstract TypedQuery<Book> findByTitle(EntityManager entityManager, String title);

    @HQL("from Book where title like ?1 order by title fetch first ?2 rows only")
    abstract List<Book> findFirstNByTitle(Session session, String title, int N);


}
