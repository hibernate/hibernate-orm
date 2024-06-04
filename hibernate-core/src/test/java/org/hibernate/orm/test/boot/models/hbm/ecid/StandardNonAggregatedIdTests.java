/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.ecid;

import java.util.List;

import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name= MappingSettings.TRANSFORM_HBM_XML, value="true"))
@DomainModel(xmlMappings = "mappings/models/hbm/ecid/standard-ecid.hbm.xml")
@SessionFactory
public class StandardNonAggregatedIdTests {
	@Test
	void simpleTest(SessionFactoryScope scope) {
		final Login login = new Login( "prod", "john", "john@doe.com" );
		scope.inTransaction( (session) -> session.persist( login ) );

		scope.inTransaction( (session) -> {
			final List<Login> logins = session.createSelectionQuery( "from Login", Login.class ).list();
			assertThat( logins ).hasSize( 1 );
		} );
	}
}
