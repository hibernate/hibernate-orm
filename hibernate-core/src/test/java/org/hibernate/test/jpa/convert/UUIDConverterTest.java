/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.convert;

import java.util.UUID;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
public class UUIDConverterTest extends BaseEntityManagerFunctionalTestCase {

	private UUID uuid = UUID.randomUUID();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15097")
	public void testSqlTypeDescriptorForConverted() {
		// persist the record.
		Integer rowId = doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = new TestEntity();
			e.setSomeValue( new SomeValue( uuid = UUID.randomUUID() ) );
			entityManager.persist( e );
			return e.getId();
		} );

		// retrieve the record and verify values.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity e = entityManager.find( TestEntity.class, rowId );
			assertEquals( uuid, e.getSomeValue().uuid );
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@Convert(converter = UUIDConverter.class)
		private SomeValue someValue;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SomeValue getSomeValue() {
			return someValue;
		}

		public void setSomeValue(SomeValue someValue) {
			this.someValue = someValue;
		}

	}

	public static class UUIDConverter implements AttributeConverter<SomeValue, UUID> {
		@Override
		public UUID convertToDatabaseColumn(SomeValue attribute) {
			return attribute == null ? null : attribute.uuid;
		}

		@Override
		public SomeValue convertToEntityAttribute(UUID dbData) {
			return dbData == null ? null : new SomeValue( dbData );
		}
	}

	public static class SomeValue {
		private final UUID uuid;

		public SomeValue(UUID uuid) {
			this.uuid = uuid;
		}

		public UUID getUuid() {
			return uuid;
		}
	}
}
