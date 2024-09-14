/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cid.nonaggregated.dynamic;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Note that this test uses a composite-id with key-many-to-one as part of a
 * dynamic model, which is the main construct needed by hibernate-envers
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/bootstrap/binding/hbm/cid/nonaggregated/dynamic/DynamicCompositeIdManyToOne.hbm.xml" )
@SessionFactory
public class DynamicCompositeIdManyToOneUsageTests {
	@Test
	public void testFullQueryReference(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__" ).list();
					session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__ where e__.id.key1 = 1" ).list();
					session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__ where e__.id.key2.name = 'abc'" ).list();
				}
		);
	}

	@Test
	@FailureExpected( reason = "Do we want to allow this?" )
	public void testEmbeddedQueryReference(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__" ).list();
					session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__ where e__.key1 = 1" ).list();
					session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__ where e__.key2.name = 'abc'" ).list();
				}
		);
	}
}
