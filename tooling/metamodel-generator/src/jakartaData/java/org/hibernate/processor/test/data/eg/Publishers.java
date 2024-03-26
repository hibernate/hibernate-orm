package org.hibernate.processor.test.data.eg;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface Publishers extends BasicRepository<Publisher,Long> {
}
