/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;

/**
 * @author Chris Cranford
 */
public abstract class AbstractRevisionEndTimestampTest extends BaseEnversJPAFunctionalTestCase {

	private static final String TIMESTAMP_FIELD = "REVEND_TSTMP";

	@Override
	@SuppressWarnings("unchecked")
	public void addConfigOptions(Map options) {
		options.put( EnversSettings.AUDIT_TABLE_SUFFIX, "_AUD" );
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME, TIMESTAMP_FIELD );
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, "true" );
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_LEGACY_PLACEMENT, "false" );
	}

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getRevisions(Class<?> clazz, Integer id) {
		String sql = String.format( "SELECT e FROM %s_AUD e WHERE e.originalId.id = :id", clazz.getName() );
		return getEntityManager().createQuery( sql ).setParameter( "id", id ).getResultList();
	}

	protected void verifyRevisionEndTimestampsInSubclass(Class<?> clazz, Integer id) {
		final List<Map<String, Object>> entities = getRevisions( clazz, id );
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
				final Dialect dialect = getDialect();
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
