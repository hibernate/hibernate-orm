/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.jakarta;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Make sure {@code `jakarta.persistence.*`} settings have function precedence over
 * the legacy {@link `javax.persistence.*`} counterparts
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop" ),
				@Setting( name = AvailableSettings.HBM2DDL_DATABASE_ACTION, value = "none" )
		}
)
@DomainModel( annotatedClasses = SimpleEntity.class )
@SessionFactory
public class JakartaSchemaToolingSettingPrecedenceTests {
	@Test
	public void verifySchemaCreated(SessionFactoryScope scope) {
		// the query would fail if the schema were not exported - just a smoke test
		scope.inTransaction( (s) -> s.createQuery( "from SimpleEntity" ).list() );
	}
}
