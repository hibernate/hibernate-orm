/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.columntransformer;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.columntransformer.Staff;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * @author Brian Parker
 */
@Disabled("NYI - NativeQuery and Parameter Bindings")
public class ReadWriteExpressionChangeTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private static final Double HEIGHT_INCHES = 73.0d;
	private static final Double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;

	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Staff.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					Staff staff = new Staff( HEIGHT_INCHES, 1 );
					entityManager.persist( staff );
					return staff.getId();
				}
		);
	}

	@DynamicTest
	public void shouldRespectWriteExpression() {
		final Double sizeInCm = inTransaction(
				entityManager -> {
					final List resultList = entityManager
							.createNativeQuery( "select size_in_cm from t_staff_AUD where id = ?" )
							.setParameter( 0, id )
							.getResultList();

					assertThat( resultList, CollectionMatchers.hasSize( 1 ) );

					return resolveSizeInCentimeters( resultList.get( 0 ), getDialect() );
				}
		);
		assertThat( sizeInCm.doubleValue(), closeTo( HEIGHT_CENTIMETERS, 0.00000001 ) );
	}

	@DynamicTest
	public void shouldRespectReadExpression() {
		final List<Number> revisions = getAuditReader().getRevisions( Staff.class, id );
		assertThat( revisions, CollectionMatchers.hasSize( 1 ) );

		final Staff rev = getAuditReader().find( Staff.class, id, revisions.get( 0 ) );
		assertThat( rev.getSizeInInches(), closeTo( HEIGHT_INCHES, 0.00000001 ) );
	}

	private static Double resolveSizeInCentimeters(Object value, Dialect dialect) {
		if ( dialect instanceof Oracle8iDialect ) {
			return ( (BigDecimal) value ).doubleValue();
		}
		else {
			return (Double) value;
		}
	}
}
