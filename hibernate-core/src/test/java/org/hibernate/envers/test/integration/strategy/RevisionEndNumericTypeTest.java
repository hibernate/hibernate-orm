/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.strategy;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.RequiresAuditStrategy;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.TestForIssue;
import org.hibernate.type.LongType;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * Validates that when setting the {@link EnversSettings#USE_NUMERIC_REVEND_TIMESTAMP_FIELD_TYPE} is enabled
 * that the metadata reflects the field type is {@link LongType}.
 *
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-6210" )
@RequiresAuditStrategy(value = ValidityAuditStrategy.class, jiraKey = "HHH-6210")
public class RevisionEndNumericTypeTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, "true" );
		options.put( EnversSettings.USE_NUMERIC_REVEND_TIMESTAMP_FIELD_TYPE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@Test
	public void testRevisionEndTimestampIsLongType() {
		// get the entity and verify the revision end timestamp property exists
		final PersistentClass clazz = metadata().getEntityBinding( StrTestEntity.class.getName() + "_AUD" );
		assertTrue( clazz.hasProperty( getAuditServiceOptions().getRevisionEndTimestampFieldName() ) );
		// get the revision end timestamp property and confirm it is of Long type.
		final Property property = clazz.getProperty( getAuditServiceOptions().getRevisionEndTimestampFieldName() );
		assertTyping( LongType.class, property.getType() );
	}
}
