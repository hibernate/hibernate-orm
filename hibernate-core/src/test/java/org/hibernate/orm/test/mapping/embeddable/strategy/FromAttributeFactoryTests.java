/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.embeddable.strategy;

import org.hibernate.testing.orm.domain.gambit.EntityOfComposites;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = EntityOfComposites.class )
@SessionFactory
public class FromAttributeFactoryTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getRuntimeMetamodels();
	}
}
