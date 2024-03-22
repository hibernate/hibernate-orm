package org.hibernate.processor.test.data.superdao.generic;

import jakarta.data.repository.Repository;
import org.hibernate.annotations.processing.Find;

@Repository
public interface Repo extends SuperRepo<Book,String> {
    @Find
	Book get(String isbn);
}
