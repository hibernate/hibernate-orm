/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {DateTimeParameterTest.Thing.class})
public class DateTimeParameterTest {
	private static GregorianCalendar nowCal = new GregorianCalendar();
	private static Date now = new Date( nowCal.getTime().getTime() );

	@Test
	public void testBindingCalendarAsDate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createQuery( "from Thing t where t.someDate = :aDate" );
			query.setParameter( "aDate", nowCal, TemporalType.DATE );
			final List list = query.getResultList();
			assertThat( list.size() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testBindingNulls(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createQuery(
					"from Thing t where t.someDate = :aDate or t.someTime = :aTime or t.someTimestamp = :aTimestamp"
			);
			query.setParameter( "aDate", (Date) null, TemporalType.DATE );
			query.setParameter( "aTime", (Date) null, TemporalType.DATE );
			query.setParameter( "aTimestamp", (Date) null, TemporalType.DATE );
		} );

	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17151")
	public void testBindingNullNativeQueryPositional(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery( "update Thing set someDate = ?1 where id = 1" );
			//noinspection deprecation
			query.setParameter( 1, (Date) null, TemporalType.DATE );
			assertThat( query.executeUpdate() ).isEqualTo( 1 );
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Thing.class, 1 ).someDate ).isNull() );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17151")
	public void testBindingNullNativeQueryNamed(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery( "update Thing set someDate = :me where id = 1" );
			Parameter<Date> p = new Parameter<>() {
				@Override
				public String getName() {
					return "me";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class<Date> getParameterType() {
					return Date.class;
				}
			};
			//noinspection deprecation
			query.setParameter( p, null, TemporalType.DATE );
			assertThat( query.executeUpdate() ).isEqualTo( 1 );
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Thing.class, 1 ).someDate ).isNull() );
	}

	@BeforeEach
	public void createTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new Thing( 1, "test", now, now, now ) ) );
	}

	@AfterEach
	public void deleteTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from Thing" ).executeUpdate() );
	}

	@Entity(name = "Thing")
	@Table(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;
		public String someString;
		@Temporal(TemporalType.DATE)
		public Date someDate;
		@Temporal(TemporalType.TIME)
		public Date someTime;
		@Temporal(TemporalType.TIMESTAMP)
		public Date someTimestamp;

		public Thing() {
		}

		public Thing(Integer id, String someString, Date someDate, Date someTime, Date someTimestamp) {
			this.id = id;
			this.someString = someString;
			this.someDate = someDate;
			this.someTime = someTime;
			this.someTimestamp = someTimestamp;
		}
	}
}
