/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.attributeoverride;

import java.time.LocalTime;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.MapAttribute;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;
import static jakarta.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				Schedule.class,
				Route.class
		}
)
@SessionFactory
class MapAttributeOverrideTest {

	@Test
	@JiraKey("HHH-18516")
	void testMapOfEmbeddableKeysWithAttributeOverridesAndBasicValues(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction(session -> {
			EntityType<Schedule> scheduleType = session.getMetamodel().entity(Schedule.class);
			MapAttribute<? super Schedule, ?, ?> departuresMapAttribute = scheduleType.getMap("departures");
			// Presence of @AttributeOverride-s for the key should only affect the type of the key, but not the type of the value
			assertThat(departuresMapAttribute.getKeyType().getPersistenceType()).isEqualTo(EMBEDDABLE);
			assertThat(departuresMapAttribute.getElementType().getPersistenceType()).isEqualTo(BASIC);

			session.persist(new Schedule(Map.of(
					new Route("Hamburg", "Vienna"), LocalTime.NOON,
					new Route("Warsaw", "Barcelona"), LocalTime.MIDNIGHT
			)));
			assertThat(session.createQuery("FROM Schedule s", Schedule.class).getResultCount()).isOne();
		});
	}
}
