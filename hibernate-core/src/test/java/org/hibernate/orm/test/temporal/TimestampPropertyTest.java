/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.temporal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
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
 * Tests that persisted timestamp properties have the expected format to milliseconds
 * and tests that entities can be queried by timestamp value.
 * <p>
 * See Mysql57TimestampFspTest for tests using MySQL 5.7. MySQL 5.7 is tested separately
 * because it requires CURRENT_TIMESTAMP(6) or NOW(6) as a default.
 *
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class, comment = "Without this, we might not see an update to the timestamp")
@RequiresDialectFeature( feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class )
@DomainModel(
		annotatedClasses = TimestampPropertyTest.Entity.class
)
@SessionFactory
public class TimestampPropertyTest {
	private final DateFormat timestampFormat = new SimpleDateFormat( "HH:mm:ss.SSS" );

	@Test
	public void testTime(SessionFactoryScope scope) {
		final Entity eOrig = new Entity();
		eOrig.ts = new Date();

		scope.inTransaction(
				session ->
						session.persist( eOrig )
		);

		scope.inTransaction(
				session -> {
					final Entity eGotten = session.get( Entity.class, eOrig.id );
					final String tsOrigFormatted = timestampFormat.format( eOrig.ts );
					final String tsGottenFormatted = timestampFormat.format( eGotten.ts );
					assertEquals( tsOrigFormatted, tsGottenFormatted );
				}
		);

		scope.inTransaction(
				session -> {
					final Query<Entity> queryWithParameter = session.createQuery(
									"from TimestampPropertyTest$Entity where ts=?1" )
							.setParameter(
									1,
									eOrig.ts
							);
					final Entity eQueriedWithParameter = queryWithParameter.uniqueResult();
					assertNotNull( eQueriedWithParameter );
				}
		);

		final Entity eQueriedWithTimestamp = scope.fromTransaction(
				session -> {
					final Query<Entity> queryWithTimestamp = session.createQuery(
									"from TimestampPropertyTest$Entity where ts=?1" )
							.setParameter(
									1,
									eOrig.ts,
									StandardBasicTypes.TIMESTAMP
							);
					final Entity queryResult = queryWithTimestamp.uniqueResult();
					assertNotNull( queryResult );
					return queryResult;
				}
		);

		scope.inTransaction(
				session ->
						session.remove( eQueriedWithTimestamp )
		);
	}

	@Test
	public void testTimeGeneratedByColumnDefault(SessionFactoryScope scope) {
		final Entity eOrig = new Entity();

		scope.inTransaction(
				session ->
						session.persist( eOrig )
		);

		assertNotNull( eOrig.tsColumnDefault );

		scope.inTransaction(
				session -> {
					final Entity eGotten = session.get( Entity.class, eOrig.id );
					final String tsColumnDefaultOrigFormatted = timestampFormat.format( eOrig.tsColumnDefault );
					final String tsColumnDefaultGottenFormatted = timestampFormat.format( eGotten.tsColumnDefault );
					assertEquals( tsColumnDefaultOrigFormatted, tsColumnDefaultGottenFormatted );
				}
		);

		scope.inTransaction(
				session -> {
					final Query<Entity> queryWithParameter =
							session.createQuery( "from TimestampPropertyTest$Entity where tsColumnDefault=?1" )
									.setParameter( 1, eOrig.tsColumnDefault );
					final Entity eQueriedWithParameter = queryWithParameter.uniqueResult();
					assertNotNull( eQueriedWithParameter );
				}
		);

		final Entity eQueriedWithTimestamp = scope.fromTransaction(
				session -> {
					final Query<Entity> queryWithTimestamp =
							session.createQuery( "from TimestampPropertyTest$Entity where tsColumnDefault=?1" )
									.setParameter( 1, eOrig.tsColumnDefault, StandardBasicTypes.TIMESTAMP );
					final Entity queryResult = queryWithTimestamp.uniqueResult();
					assertNotNull( queryResult );
					return queryResult;
				}
		);

		scope.inTransaction(
				session ->
						session.remove( eQueriedWithTimestamp )
		);
	}

	@jakarta.persistence.Entity
	@Table(name = "MyEntity")
	public static class Entity {
		@GeneratedValue
		@Id
		private long id;

		@Temporal(value = TemporalType.TIMESTAMP)
		private Date ts;

		@Temporal(value = TemporalType.TIMESTAMP)
		@Generated
		@ColumnDefault(value = "CURRENT_TIMESTAMP")
		private Date tsColumnDefault;
	}
}
