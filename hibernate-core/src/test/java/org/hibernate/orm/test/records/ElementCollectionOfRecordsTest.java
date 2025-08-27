/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OrderBy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@SessionFactory
@DomainModel(annotatedClasses = {ElementCollectionOfRecordsTest.MainEntity.class})
public class ElementCollectionOfRecordsTest {

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18957" )
	public void testInsertOrderOfRecordsInElementCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MainEntity me = new MainEntity();
					me.setId( 1L );
					me.addRecord( new Record( "c", "a", "b", 2L ) );
					me.addRecord( new Record( "2c", "a2", "bb", 22L ) );
					session.persist( me );
				}
		);
		scope.inTransaction(
				session -> {
					MainEntity me = session.find( MainEntity.class, 1L );
					List<Record> records = me.getRecords();
					assertEquals(2, records.size());
					Record r = records.get( 0 );
					assertEquals("a", r.aField);
					assertEquals("b", r.bField);
					assertEquals("c", r.cField);
					assertEquals(2L, r.longField);
					r = records.get( 1 );
					assertEquals("a2", r.aField);
					assertEquals("bb", r.bField);
					assertEquals("2c", r.cField);
					assertEquals(22L, r.longField);
				}
		);
	}

	@Entity(name = "MainEntity")
	public static class MainEntity {
		@Id
		Long id;

		@OrderColumn
		@ElementCollection(fetch = FetchType.EAGER)
		@OrderBy("longField")
		List<Record> records = new ArrayList<>();

		public void setId(Long id) {
			this.id = id;
		}

		public void addRecord(Record r) {
			this.records.add( r );
		}

		public List<Record> getRecords() {
			return records;
		}
	}

	@Embeddable
	public record Record(String cField, String aField, String bField, Long longField) {}
}
