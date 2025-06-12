/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = Record.class)
public class TypesafeNamedQueryTest {
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "'order by timestamp, id' not quite working")
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Record record1 = new Record("Hello, World!");
			Record record2 = new Record("Goodbye!");
			// make sure record2 timestamp is actually different from record1 to prevent test failure
			record2.timestamp = record1.timestamp.plusNanos( scope.getDialect().getFractionalSecondPrecisionInNanos() );
			entityManager.persist(record1);
			entityManager.persist(record2);
			String text = entityManager.createQuery(Record_._TextById_).setParameter(1, record1.id).getSingleResult();
			assertEquals("Hello, World!", text);
			List<Record> all = entityManager.createQuery(Record_._AllRecords_).getResultList();
			assertEquals(2, all.size());
			List<Object[]> tuples = entityManager.createQuery(Record_._AllRecordsAsTuples_).getResultList();
			assertEquals(2, all.size());
			assertEquals(tuples.get(0)[0], record1.id);
			assertEquals(tuples.get(1)[1], record2.text);
			Record record = entityManager.find(Record_._CompleteRecord, record2.id);
			assertEquals("Goodbye!", record.text);
		});
	}
}
