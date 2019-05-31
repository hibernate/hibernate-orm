/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.strategy;

import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.SetOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.SetOwningEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicOrder;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.junit5.envers.RequiresAuditStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the ValidityAuditStrategy on many-to-many Sets.
 * It was first introduced because of a bug when adding and removing the same element
 * from the set multiple times between database persists.
 * Created on: 24.05.11
 *
 * @author Oliver Lorenz
 * @since 3.6.5
 */
@RequiresAuditStrategy(ValidityAuditStrategy.class)
public class ValidityAuditStrategyManyToManyTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private Integer ing_id;

	private Integer ed_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetOwningEntity.class, SetOwnedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		entityManagerFactoryScope().inTransactionsWithClear(
				// Revision 1: Initial persist
				entityManager -> {
					final SetOwningEntity setOwningEntity = new SetOwningEntity( 1, "parent" );
					final SetOwnedEntity setOwnedEntity = new SetOwnedEntity( 2, "child" );
					entityManager.persist( setOwningEntity );
					entityManager.persist( setOwnedEntity );

					this.ing_id = setOwningEntity.getId();
					this.ed_id = setOwnedEntity.getId();
				},

				// Revision 2: Add child for first time
				entityManager -> {
					SetOwningEntity owningEntity = entityManager.find( SetOwningEntity.class, ing_id );
					SetOwnedEntity ownedEntity = entityManager.find( SetOwnedEntity.class, ed_id );

					owningEntity.setReferences( new HashSet<>( Collections.singletonList( ownedEntity )) );
				},

				// Revision 3; Remove child
				entityManager -> {
					SetOwningEntity owningEntity = entityManager.find( SetOwningEntity.class, ing_id );
					SetOwnedEntity ownedEntity = entityManager.find( SetOwnedEntity.class, ed_id );

					owningEntity.getReferences().remove( ownedEntity );
				},

				// Revision 4: Add child again
				entityManager -> {
					SetOwningEntity owningEntity = entityManager.find( SetOwningEntity.class, ing_id );
					SetOwnedEntity ownedEntity = entityManager.find( SetOwnedEntity.class, ed_id );

					owningEntity.getReferences().add( ownedEntity );
				},

				// Revision 5: Remove child again
				entityManager -> {
					SetOwningEntity owningEntity = entityManager.find( SetOwningEntity.class, ing_id );
					SetOwnedEntity ownedEntity = entityManager.find( SetOwnedEntity.class, ed_id );

					owningEntity.getReferences().remove( ownedEntity );
				}
		);
	}

	@DynamicTest
	@DynamicOrder(1)
	public void testMultipleAddAndRemove() {
		inTransaction(
				entityManager -> {
					SetOwningEntity owningEntity = entityManager.find( SetOwningEntity.class, ing_id );
					assertThat( owningEntity.getReferences(), CollectionMatchers.isEmpty() );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetOwningEntity.class, ing_id ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( SetOwnedEntity.class, ed_id ), contains( 1, 2, 3, 4, 5 ) );
	}

	@DynamicTest
	public void testHistoryOfIng1() {
		SetOwningEntity ver_empty = SetOwningEntity.from( ing_id, "parent" );
		SetOwningEntity ver_child = SetOwningEntity.from( ing_id, "parent", new SetOwnedEntity( ed_id, "child" ) );

		assertThat( getAuditReader().find( SetOwningEntity.class, ing_id, 1 ), equalTo( ver_empty ) );
		assertThat( getAuditReader().find( SetOwningEntity.class, ing_id, 2 ), equalTo( ver_child ) );
		assertThat( getAuditReader().find( SetOwningEntity.class, ing_id, 3 ), equalTo( ver_empty ) );
		assertThat( getAuditReader().find( SetOwningEntity.class, ing_id, 4 ), equalTo( ver_child ) );
		assertThat( getAuditReader().find( SetOwningEntity.class, ing_id, 5 ), equalTo( ver_empty ) );
	}

	@DynamicTest
	public void testHistoryOfEd1() {
		SetOwnedEntity ver_empty = SetOwnedEntity.from( ed_id, "child" );
		SetOwnedEntity ver_child = SetOwnedEntity.from( ed_id, "child", new SetOwningEntity( ing_id, "parent" ) );

		assertThat( getAuditReader().find( SetOwnedEntity.class, ed_id, 1 ), equalTo( ver_empty ) );
		assertThat( getAuditReader().find( SetOwnedEntity.class, ed_id, 2 ), equalTo( ver_child ) );
		assertThat( getAuditReader().find( SetOwnedEntity.class, ed_id, 3 ), equalTo( ver_empty ) );
		assertThat( getAuditReader().find( SetOwnedEntity.class, ed_id, 4 ), equalTo( ver_child ) );
		assertThat( getAuditReader().find( SetOwnedEntity.class, ed_id, 5 ), equalTo( ver_empty ) );
	}
}
