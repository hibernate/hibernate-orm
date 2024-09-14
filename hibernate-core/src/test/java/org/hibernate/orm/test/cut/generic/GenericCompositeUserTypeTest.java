/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cut.generic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


@JiraKey(value = "HHH-17019")
@DomainModel(
		annotatedClasses = {
				GenericCompositeUserTypeEntity.class
		}
)
@SessionFactory
public class GenericCompositeUserTypeTest {

	@Test
	public void hhh17019Test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EnumPlaceholder<Weekdays, Weekdays> placeholder = new EnumPlaceholder<>( Weekdays.MONDAY, Weekdays.SUNDAY );
			GenericCompositeUserTypeEntity entity = new GenericCompositeUserTypeEntity( placeholder );

			session.persist( entity );
		} );
	}
}
