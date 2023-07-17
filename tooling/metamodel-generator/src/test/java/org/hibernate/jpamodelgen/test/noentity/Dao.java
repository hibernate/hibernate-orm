package org.hibernate.jpamodelgen.test.noentity;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.HQL;

public interface Dao {
    @HQL("select upper('Hibernate')")
    TypedQuery<String> getName();
}
