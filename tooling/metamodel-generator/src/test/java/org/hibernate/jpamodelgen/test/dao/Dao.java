package org.hibernate.jpamodelgen.test.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import java.util.List;

public interface Dao {

    EntityManager getEntityManager();

    @Find
    Book getBook(String isbn);

    @HQL("from Book where title like ?1")
    TypedQuery<Book> findByTitle(String title);

    @HQL("from Book where title like ?1 order by title fetch first ?2 rows only")
    List<Book> findFirstNByTitle(String title, int N);
//
//    @HQL("from Book where title like :title")
//    List<Book> findByTitleWithPagination(String title, Order<? super Book> order, Page page);
//
//    @HQL("from Book where title like :title")
//    SelectionQuery<Book> findByTitleWithOrdering(String title, List<Order<? super Book>> order);
//
//    @HQL("from Book where title like :title")
//    SelectionQuery<Book> findByTitleWithOrderingByVarargs(String title, Order<? super Book>... order);

    @HQL("from Book where isbn = :isbn")
    Book findByIsbn(String isbn);

    @SQL("select * from Book where isbn = :isbn")
    Book findByIsbnNative(String isbn);
}
