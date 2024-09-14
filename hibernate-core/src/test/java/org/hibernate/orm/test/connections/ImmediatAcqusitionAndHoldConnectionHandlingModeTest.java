/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.connections;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@DomainModel
@SessionFactory
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.CONNECTION_HANDLING, value = "IMMEDIATE_ACQUISITION_AND_HOLD")
)
@Jira( "HHH-17370" )
public class ImmediatAcqusitionAndHoldConnectionHandlingModeTest {

	@Test
	public void testIt(SessionFactoryScope scope){
		// this is enough to reproduce the issue
	}
}
