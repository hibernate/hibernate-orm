/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.abstractembeddedcomponents.propertyref;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/abstractembeddedcomponents/propertyref/Mappings.hbm.xml"
)
@SessionFactory
public class AbstractComponentPropertyRefTest {
	public String[] getMappings() {
		return new String[] {};
	}

	@Test
	public void testPropertiesRefCascades(SessionFactoryScope scope) {
		ServerImpl server = new ServerImpl();
		AddressImpl address = new AddressImpl();
		scope.inTransaction(
				session -> {
					session.persist( server );
					server.setAddress( address );
					address.setServer( server );
					session.flush();
					session.createQuery( "from Server s join fetch s.address" ).list();
				}
		);

		assertNotNull( server.getId() );
		assertNotNull( address.getId() );

		scope.inTransaction(
				session -> {
					session.remove( address );
					session.remove( server );
				}
		);
	}
}
