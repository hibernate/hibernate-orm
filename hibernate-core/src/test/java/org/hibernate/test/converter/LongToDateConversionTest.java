/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10959")
public class LongToDateConversionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		try (Session session = openSession()) {
			session.getTransaction().begin();
			TestEntity entity = new TestEntity();
			entity.setDate( new DateAttribute( System.currentTimeMillis() ) );
			try {
				session.persist( entity );
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void cleanupTestData() throws Exception {
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				session.createQuery( "delete from TestEntity" ).executeUpdate();
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testSetParameter() throws Exception {
		try (Session session = openSession()) {
			final Query<TestEntity> query = session.createQuery(
					"SELECT e FROM TestEntity e WHERE e.date <= :ts",
					TestEntity.class
			).setParameter( "ts", new DateAttribute( System.currentTimeMillis() ), TemporalType.TIMESTAMP );

			final Stream<TestEntity> stream = query.stream();

			assertThat( stream.count(), is( 1L ) );
		}
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private long id;

		@Convert(converter = DateAttributeConverter.class)
		private DateAttribute date;

		public DateAttribute getDate() {
			return date;
		}

		public void setDate(DateAttribute date) {
			this.date = date;
		}
	}

	public static class DateAttribute implements Serializable {
		private long field;

		public DateAttribute(long field) {
			this.field = field;
		}
	}

	public static class DateAttributeConverter implements AttributeConverter<DateAttribute, Date> {

		@Override
		public Date convertToDatabaseColumn(DateAttribute attribute) {
			if ( attribute == null ) {
				return null;
			}
			return new Date( attribute.field );
		}

		@Override
		public DateAttribute convertToEntityAttribute(Date dbData) {
			if ( dbData == null ) {
				return null;
			}
			return new DateAttribute( dbData.getTime() );
		}

	}
}
