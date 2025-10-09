/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import java.util.Random;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.ForceLobAsLastValue.class)
@JiraKey(value = "HHH-8382")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/LobAsLastValue.hbm.xml"
)
@SessionFactory
public class LobAsLastValueTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testInsertLobAsLastValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			byte[] details = new byte[4000];
			byte[] title = new byte[2000];

			Random random = new Random();
			random.nextBytes( details );
			random.nextBytes( title );

			// This insert will fail on Oracle without the fix to ModelBinder flagging SimpleValue and Property as Lob
			// because the fields will not be placed at the end of the insert, resulting in an Oracle failure.
			final LobAsLastValueEntity entity = new LobAsLastValueEntity(
					"Test",
					new String( details ),
					new String( title )
			);
			session.persist( entity );
		} );
	}

}
