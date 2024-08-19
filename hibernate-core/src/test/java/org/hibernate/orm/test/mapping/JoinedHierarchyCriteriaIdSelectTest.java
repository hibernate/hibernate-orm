/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

@DomainModel(
		annotatedClasses = {
				JoinedHierarchyCriteriaIdSelectTest.DooredVehicle.class,
				JoinedHierarchyCriteriaIdSelectTest.BaseVehicle.class,
				JoinedHierarchyCriteriaIdSelectTest.BaseEntity.class,
		}
)
@SessionFactory
public class JoinedHierarchyCriteriaIdSelectTest {

	@ParameterizedTest
	@ValueSource(
			classes = {
					JoinedHierarchyCriteriaIdSelectTest.DooredVehicle.class,
					JoinedHierarchyCriteriaIdSelectTest.BaseVehicle.class
			}
	)
	@Jira("https://hibernate.atlassian.net/browse/HHH-18503")
	void testSelectCriteriaId(Class<?> klass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );
			final Root<?> root = criteriaQuery.from( klass );
			final Path<Long> idPath = root.get( "id" );
			criteriaQuery.select( idPath );
			final List<Long> resultList = session.createQuery( criteriaQuery ).getResultList();
			assertThat( resultList ).hasSize( 1 ).containsOnly( 1L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( DooredVehicle.create( 1L ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BaseEntity" ).executeUpdate() );
	}


	@Entity(name = "DooredVehicle")
	public static class DooredVehicle extends BaseVehicle {
		public String doorType;

		public static DooredVehicle create(Long id) {
			DooredVehicle vehicle = new DooredVehicle();
			vehicle.id = id;
			return vehicle;
		}
	}

	@Entity(name = "BaseVehicle")
	public static class BaseVehicle extends BaseEntity {
		public String bodyType;
	}

	@Entity(name = "BaseEntity")
	@DiscriminatorColumn(name = "type")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseEntity implements Serializable {

		@Id
		public Long id;

	}
}
