/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/id/Product.hbm.xml"
)
@ServiceRegistry(settings = @Setting(name = Environment.USE_IDENTIFIER_ROLLBACK, value = "true"))
@SessionFactory
public class UseIdentifierRollbackTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Product" ).executeUpdate()
		);
	}

	@Test
	public void testSimpleRollback(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product();
					assertNull( prod.getName() );
					session.persist( prod );
					session.flush();
					assertNotNull( prod.getName() );
				}
		);
	}
}
