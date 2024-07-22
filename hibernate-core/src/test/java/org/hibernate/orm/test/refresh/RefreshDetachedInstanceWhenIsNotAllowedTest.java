/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.refresh;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.refresh.TestEntity;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = TestEntity.class
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY, value = "false")
)
public class RefreshDetachedInstanceWhenIsNotAllowedTest {
	private TestEntity testEntity;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		testEntity = new TestEntity();
		scope.inTransaction(
				session ->
						session.persist( testEntity )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from TestEntity" ).executeUpdate()
		);
	}

	@Test
	public void testRefreshDetachedInstance(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						Assertions.assertThrows(
								IllegalArgumentException.class, () ->
										session.refresh( testEntity )
						)
		);
	}
}
