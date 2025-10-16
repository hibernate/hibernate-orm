/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.RequiresAuditStrategy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@code REVEND} functionality using a {@code LONG} data type.
 * This is only applicable with the ValidityAuditStrategy.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-6210")
@RequiresAuditStrategy(value = ValidityAuditStrategy.class, jiraKey = "HHH-6210")
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class})
@ServiceRegistry(settings = {
		@Setting(name = EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, value = "true"),
		@Setting(name = EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_NUMERIC, value = "true")
})
@SessionFactory
public class RevisionEndNumericTypeTest {

	@Test
	public void testRevisionEndTimestampIsTimestampType(DomainModelScope scope) {
		// get the entity and verify the revision end timestamp property exists
		final PersistentClass clazz = scope.getDomainModel().getEntityBinding( StrTestEntity.class.getName() + "_AUD" );
		assertTrue( clazz.hasProperty( "REVEND_TSTMP" ) );

		final Property property = clazz.getProperty( "REVEND_TSTMP" );
		assertTyping( BasicType.class, clazz.getProperty( "REVEND_TSTMP" ).getType() );
		assertEquals( Long.class, ((BasicType<?>) property.getType()).getJavaType() );
	}
}
