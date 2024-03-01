package org.hibernate.processor.test.hqlsql;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.query.Order;

import java.util.List;

public abstract class Books {
    @Find
    abstract Book getBook(EntityManager entityManager, String isbn);

    @Find
    abstract Book getBook(StatelessSession session, String title, String isbn);

    @Find
    abstract Book getBookByNaturalKey(Session session, String authorName, String title);

    @HQL("from Book where title is not null")
    abstract List<Book> allbooks(StatelessSession ss);

    @HQL("from Book where title like ?2")
    abstract TypedQuery<Book> findByTitle(EntityManager entityManager, String title);

    @HQL("from Book where title like ?2 order by title fetch first ?3 rows only")
    abstract List<Book> findFirstNByTitle(Session session, String title, int N);

    static class Summary { Summary(String title, String publisher, String isbn) {} }

    @HQL("select title, publisher.name, isbn from Book")
    abstract List<Summary> summarize(Session session, Order<Summary> order);
}
