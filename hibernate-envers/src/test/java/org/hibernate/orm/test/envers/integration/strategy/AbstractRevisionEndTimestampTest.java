/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Chris Cranford
 */
public abstract class AbstractRevisionEndTimestampTest {

	protected static final String TIMESTAMP_FIELD = "REVEND_TSTMP";

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getRevisions(EntityManagerFactoryScope scope, Class<?> clazz, Integer id) {
		String sql = String.format( "SELECT e FROM %s_AUD e WHERE e.originalId.id = :id", clazz.getName() );
		return scope.fromEntityManager( em ->
				em.createQuery( sql ).setParameter( "id", id ).getResultList()
		);
	}

	protected void verifyRevisionEndTimestampsInSubclass(EntityManagerFactoryScope scope, Class<?> clazz, Integer id) {
		final List<Map<String, Object>> entities = getRevisions( scope, clazz, id );
		for ( Map<String, Object> entity : entities ) {
			Object timestampParentClass = entity.get( TIMESTAMP_FIELD );
			Object timestampSubclass = entity.get( TIMESTAMP_FIELD + "_" + clazz.getSimpleName() + "_AUD" );
			SequenceIdRevisionEntity revisionEnd = (SequenceIdRevisionEntity) entity.get( "REVEND" );
			if ( timestampParentClass == null ) {
				// if the parent class has no revision end timestamp, verify that the child does not have a value
				// as well as that the revision end field is also null.
				assertNull( timestampSubclass );
				assertNull( revisionEnd );
			}
			else {
				// Verify that the timestamp in the revision entity matches that in the parent entity's
				// revision end timestamp field as well.
				final Date timestamp = (Date) timestampParentClass;
				final var dialect = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
						.getServiceRegistry().getService( JdbcServices.class ).getDialect();
				if ( dialect instanceof SybaseDialect ) {
					// Sybase DATETIME are accurate to 1/300 second on platforms that support that level of
					// granularity.
					assertEquals( timestamp.getTime() / 1000.0, revisionEnd.getTimestamp() / 1000.0, 1.0 / 300.0 );
				}
				else {
					assertEquals( timestamp.getTime(), revisionEnd.getTimestamp() );
				}

				// make sure both parent and child have the same values.
				assertEquals( timestampParentClass, timestampSubclass );
			}
		}
	}
}
