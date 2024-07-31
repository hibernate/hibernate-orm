/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.idClass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.IMPLICIT_NAMING_STRATEGY;

@DomainModel(
		annotatedClasses = MyEntity.class
)
@SessionFactory
/*
 * With this implicit naming strategy, we got the following mapping:
 *
 * create table MyEntity (
 *   id_idA bigint not null,
 *   id_idB bigint not null,
 *   _identifierMapper_idA bigint not null, <-- ??
 *   _identifierMapper_idB bigint not null, <-- ??
 *   notes varchar(255),
 *   primary key (id_idA, id_idB)
 * )
 */
@ServiceRegistry(settings = @Setting(name = IMPLICIT_NAMING_STRATEGY, value = "component-path"))
public class IdClassNamingStrategyTest {

	@Test
	@JiraKey(value = "HHH-14241")
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			MyEntity entity = new MyEntity();
			entity.setId( new MyEntityId( 739L, 777L ) );

			session.persist( entity );
		} );
	}
}
