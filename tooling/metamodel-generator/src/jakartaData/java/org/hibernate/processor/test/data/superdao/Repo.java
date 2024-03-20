package org.hibernate.processor.test.data.superdao;

import jakarta.data.repository.Repository;
import org.hibernate.annotations.processing.Find;

@Repository
public interface Repo extends SuperRepo {
    @Find
	Book get(String isbn);
}
