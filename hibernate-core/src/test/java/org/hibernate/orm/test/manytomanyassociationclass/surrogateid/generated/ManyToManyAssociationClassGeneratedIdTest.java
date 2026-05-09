/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass.surrogateid.generated;

import java.util.HashSet;

import org.hibernate.action.queue.internal.support.GraphBasedActionQueueFactory;
import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.orm.test.manytomanyassociationclass.Membership;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests on many-to-many association using an association class with a surrogate ID that is generated.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomanyassociationclass/surrogateid/generated/Mappings.hbm.xml"
)
public class ManyToManyAssociationClassGeneratedIdTest extends AbstractManyToManyAssociationClassTest {
	@Override
	public Membership createMembership(String name) {
		return new Membership( name );
	}

	@Test
	public void testRemoveAndAddEqualElement(SessionFactoryScope scope) {
		deleteMembership( getUser(), getGroup(), getMembership() );
		addMembership( getUser(), getGroup(), createMembership( "membership" ) );

		mergeAndAssertOutcome( scope, "membership" );
	}

	@Test
	public void testRemoveAndAddEqualCollection(SessionFactoryScope scope) {
		deleteMembership( getUser(), getGroup(), getMembership() );
		getUser().setMemberships( new HashSet<>() );
		getGroup().setMemberships( new HashSet<>() );
		addMembership( getUser(), getGroup(), createMembership( "membership" ) );

		mergeAndAssertOutcome( scope, "membership" );
	}

	@Test
	public void testRemoveAndAddEqualElementNonKeyModified(SessionFactoryScope scope) {
		deleteMembership( getUser(), getGroup(), getMembership() );
		Membership membershipNew = createMembership( "membership" );
		addMembership( getUser(), getGroup(), membershipNew );
		membershipNew.setName( "membership1" );

		mergeAndAssertOutcome( scope, "membership1" );
	}

	private void mergeAndAssertOutcome(SessionFactoryScope scope, String expectedMembershipName) {
		if ( graphQueueCanReorderMembershipInsert( scope ) ) {
			scope.inTransaction( session -> session.merge( getUser() ) );

			scope.inTransaction( session -> {
				var user = session.get( getUser().getClass(), getUser().getId() );
				var group = session.get( getGroup().getClass(), getGroup().getId() );
				var membership = user.getMemberships().iterator().next();

				assertEquals( "user", user.getName() );
				assertEquals( "group", group.getName() );
				assertEquals( expectedMembershipName, membership.getName() );
				assertEquals( 1, user.getMemberships().size() );
				assertEquals( 1, group.getMemberships().size() );
				assertSame( membership, group.getMemberships().iterator().next() );
				assertSame( user, membership.getUser() );
				assertSame( group, membership.getGroup() );
			} );
		}
		else {
			mergeExpectingLegacyConstraintViolation( scope );
		}
	}

	private boolean graphQueueCanReorderMembershipInsert(SessionFactoryScope scope) {
		var actionQueueFactory = scope.getSessionFactory().getActionQueueFactory();
		if ( actionQueueFactory.getConfiguredQueueType() != QueueType.GRAPH ) {
			return false;
		}

		var membershipPersister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( Membership.class );
		if ( !membershipPersister.getGenerator().generatedOnExecution() ) {
			return true;
		}

		return actionQueueFactory instanceof GraphBasedActionQueueFactory graphFactory
				&& graphFactory.deferIdentityInserts();
	}

	private void mergeExpectingLegacyConstraintViolation(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						// The new membership is transient (it has a null surrogate ID), so
						// legacy or immediate-IDENTITY insertion inserts before deleting and
						// violates the unique constraint on the user and group IDs. See HHH-2801.
						session.merge( getUser() );
						session.getTransaction().commit();
						fail( "should have failed because inserts are before deletes" );
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						// expected
						assertTyping( ConstraintViolationException.class, e );
					}
				}
		);
	}
}
