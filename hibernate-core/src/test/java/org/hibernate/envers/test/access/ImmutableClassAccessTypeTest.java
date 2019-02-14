/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.access;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.access.ClassAccessNoSettersEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class ImmutableClassAccessTypeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private ClassAccessNoSettersEntity entity = ClassAccessNoSettersEntity.of( 123, "Germany" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ClassAccessNoSettersEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction( entityManager -> { entityManager.persist( entity ); } );
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat(
				getAuditReader().getRevisions( ClassAccessNoSettersEntity.class, entity.getCode() ),
				hasItems( 1 )
		);
	}

	@DynamicTest
	public void testFindBySession() {
		inTransaction(
				session -> {
					final ClassAccessNoSettersEntity loaded = session.find(
							ClassAccessNoSettersEntity.class,
							entity.getCode()
					);

					assertThat( loaded, is( entity ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		assertThat( getAuditReader().find( ClassAccessNoSettersEntity.class, entity.getCode(), 1 ), is( entity ) );
	}
}
