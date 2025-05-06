/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.temporal;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = TimePropertyTest.Entity.class
)
@SessionFactory
public class TimePropertyTest {
	private final DateFormat timeFormat = new SimpleDateFormat( "HH:mm:ss" );

	@Test
	public void testTimeAsDate(SessionFactoryScope scope) {
		final Entity eOrig = new Entity();
		Calendar calendar = Calendar.getInstance();
		// See javadoc for java.sql.Time: 'The date components should be set to the "zero epoch" value of January 1, 1970 and should not be accessed'
		// Other dates can potentially lead to errors in JDBC drivers, in particular MySQL ConnectorJ 8.x.
		calendar.set( 1970, Calendar.JANUARY, 1 );
		// H2Dialect uses TIME (without fractional seconds precision) so H2 would round half up if milliseconds were set
		// See also: http://h2database.com/html/datatypes.html#time_type
		calendar.set( Calendar.MILLISECOND, 0 );
		eOrig.tAsDate = new Time( calendar.getTimeInMillis() );

		scope.inTransaction(
				session ->
						session.persist( eOrig )
		);

		final Entity eGotten = scope.fromTransaction(
				session -> {
					final Entity e = session.get( Entity.class, eOrig.id );
					// Some databases retain the milliseconds when being inserted and some don't;
					final String tAsDateOrigFormatted = timeFormat.format( eOrig.tAsDate );
					final String tAsDateGottenFormatted = timeFormat.format( e.tAsDate );
					assertEquals( tAsDateOrigFormatted, tAsDateGottenFormatted );
					return e;
				}
		);

		final String queryString;

		if ( SQLServerDialect.class.isAssignableFrom( scope.getSessionFactory().getJdbcServices().getDialect().getClass() ) ) {
			queryString = "from TimePropertyTest$Entity where tAsDate = cast ( ?1 as time )";
		}
		else {
			queryString = "from TimePropertyTest$Entity where tAsDate = ?1";
		}

		scope.inTransaction(
				session -> {

					final Query queryWithParameter = session.createQuery( queryString ).setParameter(
							1,
							eGotten.tAsDate
					);
					final Entity eQueriedWithParameter = (Entity) queryWithParameter.uniqueResult();
					assertNotNull( eQueriedWithParameter );
				}
		);


		final Entity eQueried = scope.fromTransaction(
				session -> {
					final Query query = session.createQuery( queryString ).setParameter(
							1,
							eGotten.tAsDate,
							StandardBasicTypes.TIME
					);
					final Entity queryResult = (Entity) query.uniqueResult();
					assertNotNull( queryResult );
					return queryResult;
				}
		);

		scope.inTransaction(
				session ->
						session.remove( eQueried )
		);
	}

	@jakarta.persistence.Entity
	@Table(name = "entity")
	public static class Entity {
		@GeneratedValue
		@Id
		private long id;

		@Temporal(value = TemporalType.TIME)
		private java.util.Date tAsDate;
	}
}
