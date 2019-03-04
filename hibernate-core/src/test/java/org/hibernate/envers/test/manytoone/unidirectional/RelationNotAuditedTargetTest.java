/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytoone.unidirectional;

import org.hibernate.Hibernate;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.manytoone.unidirectional.TargetNotAuditedEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Tomasz Bech
 */
public class RelationNotAuditedTargetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer tnae1_id;
	private Integer tnae2_id;

	private Integer uste1_id;
	private Integer uste2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TargetNotAuditedEntity.class, UnversionedStrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// No revision
				entityManager -> {
					final UnversionedStrTestEntity uste1 = new UnversionedStrTestEntity( "str1" );
					final UnversionedStrTestEntity uste2 = new UnversionedStrTestEntity( "str2" );
					entityManager.persist( uste1 );
					entityManager.persist( uste2 );

					this.uste1_id = uste1.getId();
					this.uste2_id = uste2.getId();
				},

				// Revision 1
				entityManager -> {
					final UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );
					final UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					TargetNotAuditedEntity tnae1 = new TargetNotAuditedEntity( 1, "tnae1", uste1 );
					TargetNotAuditedEntity tnae2 = new TargetNotAuditedEntity( 2, "tnae2", uste2 );

					entityManager.persist( tnae1 );
					entityManager.persist( tnae2 );

					this.tnae1_id = tnae1.getId();
					this.tnae2_id = tnae2.getId();
				},

				// Revision 2
				entityManager -> {
					final UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );
					final UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					final TargetNotAuditedEntity tnae1 = entityManager.find( TargetNotAuditedEntity.class, tnae1_id );
					final TargetNotAuditedEntity tnae2 = entityManager.find( TargetNotAuditedEntity.class, tnae2_id );

					tnae1.setReference( uste2 );
					tnae2.setReference( uste1 );
				},

				// Revision 3
				entityManager -> {
					final UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					final TargetNotAuditedEntity tnae1 = entityManager.find( TargetNotAuditedEntity.class, tnae1_id );
					final TargetNotAuditedEntity tnae2 = entityManager.find( TargetNotAuditedEntity.class, tnae2_id );

					//field not changed!!!
					tnae1.setReference( uste2 );
					tnae2.setReference( uste2 );
				},

				// Revision 4
				entityManager -> {
					final UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );

					final TargetNotAuditedEntity tnae1 = entityManager.find( TargetNotAuditedEntity.class, tnae1_id );
					final TargetNotAuditedEntity tnae2 = entityManager.find( TargetNotAuditedEntity.class, tnae2_id );

					tnae1.setReference( uste1 );
					tnae2.setReference( uste1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( TargetNotAuditedEntity.class, tnae1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( TargetNotAuditedEntity.class, tnae2_id ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfTnae1_id() {
		// load original "tnae1" TargetNotAuditedEntity to force load "str1" UnversionedStrTestEntity as Proxy
		inTransaction(
				entityManager -> {
					final TargetNotAuditedEntity original = entityManager.find( TargetNotAuditedEntity.class, tnae1_id );

					final UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );
					final UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					final TargetNotAuditedEntity rev1 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 1 );
					assertThat( rev1.getReference(), equalTo( uste1 ) );

					final TargetNotAuditedEntity rev2 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 2 );
					assertThat( rev2.getReference(), equalTo( uste2 ) );

					final TargetNotAuditedEntity rev3 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 3 );
					assertThat( rev3.getReference(), equalTo( uste2 ) );

					TargetNotAuditedEntity rev4 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 4 );
					assertThat( rev4.getReference(), equalTo( uste1 ) );

					assertThat( original.getReference(), instanceOf( HibernateProxy.class ) );
					assertThat( Hibernate.getClass( original.getReference() ), equalTo( UnversionedStrTestEntity.class ) );
					assertThat( HibernateProxyHelper.getClassWithoutInitializingProxy( rev1.getReference() ), equalTo( UnversionedStrTestEntity.class ) );
					assertThat( Hibernate.getClass( rev1.getReference() ), equalTo( UnversionedStrTestEntity.class ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfTnae2_id() {
		inTransaction(
				entityManager -> {
					final UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );
					final UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					final TargetNotAuditedEntity rev1 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 1 );
					assertThat( rev1.getReference(), equalTo( uste2 ) );

					final TargetNotAuditedEntity rev2 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 2 );
					assertThat( rev2.getReference(), equalTo( uste1 ) );

					final TargetNotAuditedEntity rev3 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 3 );
					assertThat( rev3.getReference(), equalTo( uste2 ) );

					final TargetNotAuditedEntity rev4 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 4 );
					assertThat( rev4.getReference(), equalTo( uste1 ) );
				}
		);
	}
}
