package org.hibernate.processor.test.data.quarkus;

import java.util.List;


import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface JakartaDataBookRepository extends CrudRepository<PanacheBook, Long> {
    @Find
    public List<PanacheBook> findBook(String isbn);

    @Query("WHERE isbn = :isbn")
    public List<PanacheBook> hqlBook(String isbn);
}
