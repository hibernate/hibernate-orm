/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.QueryOptions;

@Entity( name = "Jpa4StaticQueryBook" )
@NamedEntityGraph( name = "Book.summary" )
@Table( name = "jpa4_static_query_book" )
public class Book {
	@Id
	private Integer id;

	private String title;

	private String isbn;

	public Book() {
	}

	Book(Integer id, String title) {
		this.id = id;
		this.title = title;
		this.isbn = "isbn-" + id;
	}

	public String getTitle() {
		return title;
	}

	public String getIsbn() {
		return isbn;
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public List<Book> findByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "" )
	public List<Book> blankFindAll() {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public Optional<Book> optionalByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public Book[] arrayByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "select book.title from Jpa4StaticQueryBook book where book.title = :title" )
	public String[] titleArrayByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "select book.title, book.isbn from Jpa4StaticQueryBook book where book.title = :title" )
	public Object[] titleAndIsbnArray(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public TypedQuery<Book> typedQueryByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public org.hibernate.query.Query<Book> queryByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public org.hibernate.query.SelectionQuery<Book> selectionQueryByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	public org.hibernate.query.KeyedResultList<Book> keyedResultListByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "select count(book) from Jpa4StaticQueryBook book where book.title = :title" )
	public long countByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	@QueryOptions(
			cacheStoreMode = CacheStoreMode.BYPASS,
			flush = QueryFlushMode.NO_FLUSH,
			timeout = 123,
			lockMode = LockModeType.PESSIMISTIC_READ,
			entityGraph = "Book.summary"
	)
	public List<Book> findByTitleWithOptions(String title) {
		throw new UnsupportedOperationException();
	}

	@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
	public Book nativeFindByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
	@QueryOptions(
			lockMode = LockModeType.PESSIMISTIC_READ,
			lockScope = PessimisticLockScope.EXTENDED
	)
	public Book nativeFindByTitleWithOptions(String title) {
		throw new UnsupportedOperationException();
	}

	@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
	public List<Book> nativeFindAllByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@jakarta.persistence.query.NativeQuery( "select count(*) from jpa4_static_query_book where title = ?" )
	public long nativeCountByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@ColumnResult( name = "title" )
	@jakarta.persistence.query.NativeQuery( "select title from jpa4_static_query_book where title = ?" )
	public String nativeTitleByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@ColumnResult( name = "title", type = String.class )
	@ColumnResult( name = "isbn" )
	@jakarta.persistence.query.NativeQuery( "select title, isbn from jpa4_static_query_book where title = ?" )
	public List<Object[]> nativeTitleAndIsbnRows(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "delete from Jpa4StaticQueryBook where title = :title" )
	public int deleteByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@JakartaQuery( "delete from Jpa4StaticQueryBook where title = :title" )
	@QueryOptions( flush = QueryFlushMode.NO_FLUSH, timeout = 234 )
	public int deleteByTitleWithOptions(String title) {
		throw new UnsupportedOperationException();
	}

	@jakarta.persistence.query.NativeQuery( "delete from jpa4_static_query_book where title = ?" )
	public int nativeDeleteByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@jakarta.persistence.query.NativeQuery( "delete from jpa4_static_query_book where title = ?" )
	@QueryOptions( flush = QueryFlushMode.FLUSH, timeout = 345 )
	public int nativeDeleteByTitleWithOptions(String title) {
		throw new UnsupportedOperationException();
	}
}
