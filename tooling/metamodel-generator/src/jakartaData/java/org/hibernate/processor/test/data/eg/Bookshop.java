package org.hibernate.processor.test.data.eg;

import jakarta.annotation.Nonnull;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

@Repository
public interface Bookshop extends CrudRepository<Book,String> {
    @Find
    @Transactional(REQUIRES_NEW)
    List<Book> byPublisher(String publisher_name);

    @Find
    List<Book> byTitle(@Nonnull String title);

    @Query("select isbn where title like ?1 order by isbn")
    String[] ssns(@NotBlank String title);

    @Query("select count(this) where title like ?1 order by isbn")
    long count1(@NotNull String title);

    @Query("select count(this) where this.title like ?1 order by this.isbn")
    long count2(String title);

    @Query("select length(text) where title = ?1")
    int length(@Nonnull String title);

    @Query("select count(this)")
    long countAll();

    @Query("where isbn in :isbns and type = Book")
    List<Book> books(List<String> isbns);

    @Query("delete from Book where type = org.hibernate.processor.test.data.eg.Type.Book")
    long deleteAllBooks();

    @Query("delete from Book where type = Book and isbn in ?1")
    int deleteBooks(List<String> isbns);
}
