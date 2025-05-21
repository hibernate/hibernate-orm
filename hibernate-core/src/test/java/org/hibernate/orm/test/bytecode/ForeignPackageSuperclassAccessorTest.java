/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode;

import org.hibernate.orm.test.bytecode.foreignpackage.ConcreteEntity;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = {
		ConcreteEntity.class,
		SuperclassEntity.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-19369")
public class ForeignPackageSuperclassAccessorTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( SuperclassEntity.class, 1L );
		} );
	}
}
