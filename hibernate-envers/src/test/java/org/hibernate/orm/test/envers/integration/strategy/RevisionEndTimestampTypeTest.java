/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.type.BasicType;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.envers.RequiresAuditStrategy;

/**
 * Tests the {@code REVEND} functionality using a {@code LONG} data type.
 * This is only applicable with the ValidityAuditStrategy.
 *
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-6210" )
@RequiresAuditStrategy( value = ValidityAuditStrategy.class, jiraKey = "HHH-6210" )
public class RevisionEndTimestampTypeTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, "true" );
	}

	@Test
	public void testRevisionEndTimestampIsLongType() {
		// get the entity and verify the revision end timestamp property exists
		final PersistentClass clazz = metadata().getEntityBinding( StrTestEntity.class.getName() + "_AUD" );

		final Property property = clazz.getProperty( "REVEND_TSTMP" );
		assertTyping( BasicType.class, property.getType() );
		assertEquals( Timestamp.class, ( (BasicType) property.getType() ).getJavaType() );
	}
}
