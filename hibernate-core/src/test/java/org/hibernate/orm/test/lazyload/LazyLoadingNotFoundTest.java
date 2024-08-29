/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lazyload;

import org.hibernate.LazyInitializationException;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Nikolay Golubev
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Child.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"))
public class LazyLoadingNotFoundTest {

	@Test
	@TestForIssue(jiraKey = "HHH-11179")
	public void testNonExistentLazyInitOutsideTransaction(SessionFactoryScope scope) {
		Child loadedChild = scope.fromTransaction(
				session -> session.getReference( Child.class, -1L )
		);

		try {
			loadedChild.getParent();
			fail( "lazy init did not fail on non-existent proxy" );
		}
		catch (LazyInitializationException e) {
			assertNotNull( e.getMessage() );
		}
	}
}
