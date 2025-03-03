/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14276;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.orm.test.mapping.hhh14276.entity.PlayerStat;
import org.hibernate.orm.test.mapping.hhh14276.entity.Score;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

@JiraKey( value = "HHH-14276" )
public class NestedIdClassDerivedIdentifiersTest {
	@Test
	public void testMapping() {
		final Configuration configuration = new Configuration()
				.setProperty( GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE )
				.addAnnotatedClasses( PlayerStat.class, Score.class );

		try (SessionFactory sessionFactory = configuration.buildSessionFactory()) {
			sessionFactory.inTransaction( (session) -> {
				// do nothing...
			} );
		}
	}
}
