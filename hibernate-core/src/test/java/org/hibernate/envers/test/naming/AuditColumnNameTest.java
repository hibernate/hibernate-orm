/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming;

import java.util.List;

import javax.persistence.Query;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.naming.NamingTestEntity2;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Eric Haas
 */
@Disabled("NYI - NativeQuery support")
public class AuditColumnNameTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { NamingTestEntity2.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					NamingTestEntity2 nte1 = new NamingTestEntity2( "data1" );
					entityManager.persist( nte1 );
					return nte1.getId();
				}
		);
	}

	@DynamicTest
	public void testColumnName() {
		final String sql = "select nte_data, data_MOD_different from naming_test_entity_2_versions where nte_id = :id";

		inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( sql );
					query.setParameter( "id", this.id );

					@SuppressWarnings("unchecked")
					List<Object[]> resultList = query.getResultList();
					assertThat( resultList, notNullValue() );
					assertThat( resultList, CollectionMatchers.isNotEmpty() );

					final Object[] result = resultList.get( 0 );
					assertThat( result.length, equalTo( 2 ) );
				}
		);
	}
}
