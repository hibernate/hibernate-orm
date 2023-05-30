/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cid;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				ModelWithSelfChildren.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
		}
)
@SessionFactory
@JiraKey("HHH-16725")
public class CompositeIdWithOrderedUpdatesTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from ModelWithSelfChildren" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSuccessfulPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ModelWithSelfChildren m = new ModelWithSelfChildren();
					m.setString( "a" );

					session.persist( m );

					ModelWithSelfChildren m2 = new ModelWithSelfChildren();
					m2.setString( "b" );

					session.persist( m2 );
				}
		);

		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery( "from ModelWithSelfChildren", ModelWithSelfChildren.class )
									.getResultList()
					).hasSize( 2 );
				}
		);
	}
}
