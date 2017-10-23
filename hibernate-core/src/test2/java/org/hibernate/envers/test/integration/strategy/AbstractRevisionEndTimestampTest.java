/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.strategy;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQL57InnoDBDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Chris Cranford
 */
public abstract class AbstractRevisionEndTimestampTest extends BaseEnversJPAFunctionalTestCase {

	private static final String TIMESTAMP_FIELDNAME = "REVEND_TSTMP";

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.AUDIT_TABLE_SUFFIX, "_AUD" );
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME, TIMESTAMP_FIELDNAME );
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, "true" );
	}

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getRevisions(Class<?> clazz, Integer id) {
		String sql = String.format( "SELECT e FROM %s_AUD e WHERE e.originalId.id = :id", clazz.getName() );
		return getEntityManager().createQuery( sql ).setParameter( "id", id ).getResultList();
	}

	protected void verifyRevisionEndTimestamps(List<Map<String, Object>> entities) {
		for ( Map<String, Object> entity : entities ) {
			Date timestamp = (Date) entity.get( TIMESTAMP_FIELDNAME );
			SequenceIdRevisionEntity revisionEnd = (SequenceIdRevisionEntity) entity.get( "REVEND" );
			if ( timestamp == null ) {
				assertNull( revisionEnd );
			}
			else {
				final Dialect dialect = getDialect();
				if ( dialect instanceof MySQL5Dialect && !( dialect instanceof MySQL57InnoDBDialect ) ) {
					// MySQL5 DATETIME does not contain milliseconds
					// MySQL 5.7 supports millisecond precision and when MySQL57InnoDBDialect is used, it is
					// assumed that the column will be defined as DATETIME(6).
					assertEquals(
							timestamp.getTime(),
							( revisionEnd.getTimestamp() - (revisionEnd.getTimestamp() % 1000 ) )
					);
				}
				else if ( dialect instanceof SybaseASE15Dialect ) {
					// Sybase DATETIME are accurate to 1/300 second on platforms that support that level of
					// granularity.
					assertEquals( timestamp.getTime() / 1000.0, revisionEnd.getTimestamp() / 1000.0, 1.0 / 300.0 );
				}
				else {
					assertEquals( timestamp.getTime(), revisionEnd.getTimestamp() );
				}
			}
		}
	}
}
