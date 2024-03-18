package org.hibernate.processor.test.hqlsql;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.SelectionQuery;

import java.util.List;
import java.util.Map;

public interface Dao {

    @Find
    Book getBook(String isbn);

    @Find
    Book getBook(String title, String isbn);

    @Find
    Book getBookByNaturalKey(String authorName, String title);

    @HQL("from Book where title like ?1")
    TypedQuery<Book> findByTitle(String title);

    @HQL("from Book where title like ?1 order by title fetch first ?2 rows only")
    List<Book> findFirstNByTitle(String title, int N);

    @HQL("from Book where title like :title")
    List<Book> findByTitleWithPagination(String title, Order<? super Book> order, Page page);

    @HQL("from Book where title like :title")
    SelectionQuery<Book> findByTitleWithOrdering(String title, List<Order<? super Book>> order);

    @HQL("from Book where title like :title")
    SelectionQuery<Book> findByTitleWithOrderingByVarargs(String title, Order<? super Book>... order);

    @HQL("from Book where isbn = :isbn")
    Book findByIsbn(String isbn);

    @HQL("order by isbn asc, publicationDate desc")
    List<Book> allBooks();

    @HQL("order by isbn asc, publicationDate desc")
    Book[] allBooksAsArray();

    @SQL("select * from Book where isbn = :isbn")
    Book findByIsbnNative(String isbn);

    @Find
    List<Book> publishedBooks(String publisher$name);

    @HQL("from Book book join fetch book.publisher where book.title like :titlePattern")
    List<Book> booksWithPublisherByTitle(String titlePattern, Page page, Order<? super Book> order);

    @HQL("select title, pages from Book")
    List<Dto> dtoQuery();

    @HQL("select new org.hibernate.processor.test.hqlsql.Dto(title, pages) from Book")
    List<Dto> dtoQuery1();

    @HQL("select new Dto(title, pages) from Book")
    List<Dto> dtoQuery2();

    @HQL("select new map(title as title, pages as pages) from Book")
    List<Map> dtoQuery3();

    @HQL("select new list(title, pages) from Book")
    List<List> dtoQuery4();

    @HQL("from Publisher where address = :address")
    List<Publisher> publisherAt(Address address);

    @HQL("where array_contains(:isbns, isbn) is true")
    List<Book> forIsbnIn(String[] isbns);
}
