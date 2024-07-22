/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.abstractembeddedcomponents.cid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/abstractembeddedcomponents/cid/Mappings.hbm.xml"
)
@SessionFactory
public class AbstractCompositeIdTest  {

	@Test
	public void testEmbeddedCompositeIdentifierOnAbstractClass(SessionFactoryScope scope) {
		MyInterfaceImpl myInterface = new MyInterfaceImpl();
		myInterface.setKey1( "key1" );
		myInterface.setKey2( "key2" );
		myInterface.setName( "test" );

		scope.inTransaction(
				session -> {
					session.persist( myInterface );
					session.flush();

					session.createQuery( "from MyInterface" ).list();

					session.remove( myInterface );
				}
		);

	}
}
