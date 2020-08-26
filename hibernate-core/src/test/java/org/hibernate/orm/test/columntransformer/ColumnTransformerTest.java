/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.columntransformer;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel( annotatedClasses = Staff.class )
@SessionFactory
public class ColumnTransformerTest {
	public static final double ERROR = 0.01d;

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		// acts as creation validation
		scope.inTransaction(
				session -> {
					session.persist(
							new Staff( 1, 2, 3, 4 )
					);
					session.save(
							new Staff( 5, 6, 7, 8 )
					);
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		// acts as deletion validation
		scope.inTransaction(
				session -> {
					final List<Staff> list = session.createQuery( "from Staff", Staff.class ).list();
					list.forEach( session::delete );
				}
		);
	}

	@Test
	public void testBasicOperations(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Staff first = session.get( Staff.class, 4 );
					assertThat( first, notNullValue() );
					validateModelValue( first, 1, 2, 3, 4 );

					final Staff second = session.get( Staff.class, 8 );
					assertThat( second, notNullValue() );
					validateModelValue( second, 5, 6, 7, 8 );
				}
		);
	}

	private void validateModelValue(Staff staff, double sizeInInches, double radius, double diameter, int id) {
		assertThat( staff.getSizeInInches(), closeTo( sizeInInches, ERROR ) );
		assertThat( staff.getRadiusS(), closeTo( radius, ERROR ) );
		assertThat( staff.getDiameter(), closeTo( diameter, ERROR ) );
		assertThat( staff.getId(), is( id ) );
	}

	@Test
	public void testStoredValues(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sqlString =
							// represents how each is mapped in the mappings - see their @ColumnTransformer#read
							"select size_in_cm / 2.54E0"
							+ ", radiusS / 2.54d"
							+ ", diamet / 2.54E0"
							+ " from t_staff"
							+ " where t_staff.id = 4";

					final Object result = session
							.createNativeQuery( sqlString )
							.getSingleResult();
					assertThat( result, notNullValue() );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is( 3 ) );

					assertThat( ( (Number) values[0] ).doubleValue(), closeTo( 1, 0.01d ) );
					assertThat( ( (Number) values[1] ).doubleValue(), closeTo( 2, 0.01d ) );
					assertThat( ( (Number) values[2] ).doubleValue(), closeTo( 3, 0.01d ) );

				}
		);
	}

	@Test
	public void testHqlProjection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double sizeInInchesViaHql = (Double) session
							.createQuery( "select s.sizeInInches from Staff s where s.id = :id" )
							.setParameter( "id", 4 )
							.uniqueResult();
					assertThat( sizeInInchesViaHql, notNullValue() );
					assertThat( sizeInInchesViaHql, closeTo( 1,0.01d ) );
				}
		);
	}

	@Test
	public void testHqlPredicateUse(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Staff staff = session
							.createQuery( "from Staff s where s.sizeInInches between ?1 and ?2", Staff.class )
							.setParameter( 1, 5 - 0.01d )
							.setParameter( 2, 5 + 0.01d )
							.uniqueResult();

					validateModelValue( staff, 5, 6, 7, 8 );
				}
		);
	}

	@Test
	public void testCriteriaPredicateUse(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					final CriteriaQuery<Staff> criteria = criteriaBuilder.createQuery( Staff.class );
					final Root<Staff> root = criteria.from( Staff.class );
					criteria.where(
							criteriaBuilder.between(
									root.get( "sizeInInches" ),
									1 - 0.01d,
									1 + 0.01d
							)
					);

					final Staff staff = session.createQuery( criteria ).uniqueResult();
					validateModelValue( staff, 1, 2, 3, 4 );
				}
		);
	}
}
