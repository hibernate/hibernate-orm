/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
					Person me = session.load( Person.class, name );
					assertNull( me.address );
					session.remove( me );
				}
		);
	}
}
