package org.hibernate.processor.test.superdao;

import org.hibernate.annotations.processing.Find;

public interface Dao extends SuperDao {
    @Find
    Book get(String isbn);
}
