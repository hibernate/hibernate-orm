/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lazyload;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Oleksander Dukhno
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Child.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(
						name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"
				),
				@Setting(
						name = AvailableSettings.JTA_PLATFORM, value = "org.hibernate.testing.jta.TestingJtaPlatformImpl"
				),
				@Setting(
						name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"
				),
				@Setting(
						name = "javax.persistence.transactionType", value = "jta"
				)
		}
)
public class JtaLazyLoadingTest {

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope)
			throws Exception {
		Parent p = new Parent();
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
						final Child child = p.makeChild();
						session.persist( child );
						lastChildID = child.getId();
					}
					session.persist( p );
					parentID = p.getId();
				}
		);

	}

	@Test
	@TestForIssue(jiraKey = "HHH-7971")
	public void testLazyCollectionLoadingAfterEndTransaction(SessionFactoryScope scope) {
		Parent loadedParent = scope.fromTransaction(
				session ->
						session.getReference( Parent.class, parentID )

		);

		assertFalse( Hibernate.isInitialized( loadedParent.getChildren() ) );

		int i = 0;
		for ( Child child : loadedParent.getChildren() ) {
			i++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, i );

		Child loadedChild = scope.fromTransaction(
				session ->
						session.getReference( Child.class, lastChildID )
		);

		Parent p = loadedChild.getParent();
		int j = 0;
		for ( Child child : p.getChildren() ) {
			j++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, j );
	}

}
