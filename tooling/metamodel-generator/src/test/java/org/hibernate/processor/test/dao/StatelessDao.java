package org.hibernate.processor.test.dao;

import jakarta.annotation.Nullable;
import jakarta.persistence.TypedQuery;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.query.SelectionQuery;

import java.util.List;
import java.util.Optional;

public interface StatelessDao {

    StatelessSession getSession();

    @Find
    Book getBook(String isbn);

    @Find
    Optional<Book> getBookMaybe(String isbn);

    @Find(enabledFetchProfiles="Goodbye")
    Book getBookFetching(String isbn);

    @Find
    Book getBook(String title, String author);

    @Find @Nullable
    Book getBookOrNull(String isbn);

    @Find
    Optional<Book> getBookMaybe(String title, String author);

    @Find(enabledFetchProfiles="Hello")
    Book getBookFetching(String title, String author);

    @Find
    Book getBook(String title, String isbn, String author);

    @Find
    List<Book> getBooks(String title);

    @Find(enabledFetchProfiles="Hello")
    List<Book> getBooksFetching(String title);

    @Find
    SelectionQuery<Book> createBooksSelectionQuery(String title);

    @HQL("from Book where title like ?1")
    TypedQuery<Book> findByTitle(String title);

    @HQL("from Book where title like ?1")
    SelectionQuery<Book> findByTitleSelectionQuery(String title);

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
