package org.hibernate.processor.test.data.basic;

import jakarta.data.repository.Repository;

@Repository
public interface Concrete extends IdOperations<Thing> {
}
