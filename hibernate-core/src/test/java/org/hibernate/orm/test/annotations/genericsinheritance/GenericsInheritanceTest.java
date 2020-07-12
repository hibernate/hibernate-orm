/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.genericsinheritance;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = {
		ChildHierarchy1.class,
		ParentHierarchy1.class,
		ChildHierarchy22.class,
		ParentHierarchy22.class
})
@SessionFactory
public class GenericsInheritanceTest {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inSession( sessionImplementor -> {
			//mapping is tested
		} );
	}

}
