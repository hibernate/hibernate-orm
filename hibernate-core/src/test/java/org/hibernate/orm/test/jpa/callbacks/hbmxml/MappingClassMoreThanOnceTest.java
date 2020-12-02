/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks.hbmxml;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/hbmxml/ClassMappedMoreThanOnce.hbm.xml"
)
@SessionFactory
public class MappingClassMoreThanOnceTest {
	/**
	 * Tests that an entity manager can be created when a class is mapped more than once.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8775")
	public void testBootstrapWithClassMappedMOreThanOnce(SessionFactoryScope scope) {

		SessionFactoryImplementor sfi = null;
		try {
			sfi = scope.getSessionFactory();
		}
		finally {
			if ( sfi != null ) {
				try {
					sfi.close();
				}
				catch (Exception ignore) {
				}
			}
		}
	}
}
