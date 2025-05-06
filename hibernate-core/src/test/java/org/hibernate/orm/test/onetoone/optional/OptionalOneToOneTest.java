/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.optional;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/onetoone/optional/Person.hbm.xml" }
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = @Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false")
)
public class OptionalOneToOneTest {

	@Test
	public void testOptionalOneToOneRetrieval(SessionFactoryScope scope) {
		String name = scope.fromTransaction(
				session -> {
					Person me = new Person();
					me.name = "Steve";
					session.persist( me );
					return me.name;
				}
		);

		scope.inTransaction(
				session -> {
					Person me = session.getReference( Person.class, name );
					assertNull( me.address );
					session.remove( me );
				}
		);
	}
}
