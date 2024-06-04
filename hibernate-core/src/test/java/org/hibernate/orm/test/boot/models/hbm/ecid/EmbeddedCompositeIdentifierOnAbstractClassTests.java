/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.ecid;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.orm.test.abstractembeddedcomponents.cid.AbstractCompositeIdTest;
import org.hibernate.orm.test.abstractembeddedcomponents.cid.MyInterfaceImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @see AbstractCompositeIdTest
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name= MappingSettings.TRANSFORM_HBM_XML, value="true"))
@DomainModel( xmlMappings = "org/hibernate/orm/test/abstractembeddedcomponents/cid/Mappings.hbm.xml" )
@SessionFactory
@FailureExpected
public class EmbeddedCompositeIdentifierOnAbstractClassTests {

	@Test
	public void testTransformedHbmXml(SessionFactoryScope scope) {
		MyInterfaceImpl myInterface = new MyInterfaceImpl();
		myInterface.setKey1( "key1" );
		myInterface.setKey2( "key2" );
		myInterface.setName( "test" );

		scope.inTransaction(
				session -> {
					// test persistence
					session.persist( myInterface );
					session.flush();

					// test loading
					session.createQuery( "from MyInterface" ).list();
				}
		);
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete MyInterface" ).executeUpdate();
		} );
	}
}
