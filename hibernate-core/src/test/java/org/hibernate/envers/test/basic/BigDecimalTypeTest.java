/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.math.BigDecimal;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BigDecimalEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11988")
public class BigDecimalTypeTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private Integer entityId;
	private Double bigDecimalValue = 2.2d;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BigDecimalEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Triggers RevisionType.ADD
		this.entityId = inTransaction(
				entityManager -> {
					final BigDecimalEntity entity = new BigDecimalEntity( bigDecimalValue, "Test" );
					entityManager.persist( entity );
					return entity.getId();
				}
		);

		// Should *not* trigger a revision
		inTransaction(
				entityManager -> {
					final BigDecimalEntity entity = entityManager.find( BigDecimalEntity.class, entityId );
					entity.setData( "Updated" );
					entity.setBigDecimalValue( bigDecimalValue );
					entityManager.merge( entity );
				}
		);

		// Triggers RevisionType.MOD
		inTransaction(
				entityManager -> {
					final BigDecimalEntity entity = entityManager.find( BigDecimalEntity.class, entityId );
					entity.setData( "Updated2" );
					entity.setBigDecimalValue( bigDecimalValue + 1d );
					entityManager.merge( entity );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BigDecimalEntity.class, entityId ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		final BigDecimalEntity rev1 = getAuditReader().find( BigDecimalEntity.class, entityId, 1 );
		assertThat( BigDecimal.valueOf( bigDecimalValue ).compareTo( rev1.getBigDecimal() ), equalTo( 0 ) );
		assertThat( rev1.getData(), nullValue() );

		final BigDecimalEntity rev2 = getAuditReader().find( BigDecimalEntity.class, entityId, 2 );
		assertThat( BigDecimal.valueOf( bigDecimalValue + 1d ).compareTo( rev2.getBigDecimal() ), equalTo( 0 ) );
		assertThat( rev2.getData(), nullValue() );
	}
}
