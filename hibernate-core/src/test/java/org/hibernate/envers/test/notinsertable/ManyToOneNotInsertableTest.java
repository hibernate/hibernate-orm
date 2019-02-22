/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.notinsertable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.notinsertable.ManyToOneNotInsertableEntity;
import org.hibernate.envers.test.support.domains.notinsertable.NotInsertableEntityType;
import org.hibernate.envers.test.support.domains.notinsertable.NotInsertableTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyToOneNotInsertableTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer mto_id1;
	private Integer type_id1;
	private Integer type_id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ManyToOneNotInsertableEntity.class, NotInsertableEntityType.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					NotInsertableEntityType type1 = new NotInsertableEntityType( 2, "type1" );
					NotInsertableEntityType type2 = new NotInsertableEntityType( 3, "type2" );

					entityManager.persist( type1 );
					entityManager.persist( type2 );

					this.type_id1 = type1.getTypeId();
					this.type_id2 = type2.getTypeId();
				},

				// Revision 2
				entityManager -> {
					NotInsertableEntityType type1 = entityManager.find( NotInsertableEntityType.class, type_id1 );

					ManyToOneNotInsertableEntity master = new ManyToOneNotInsertableEntity( 1, type_id1, type1 );
					entityManager.persist( master );

					this.mto_id1 = master.getId();
				},

				// Revision 3
				entityManager -> {
					ManyToOneNotInsertableEntity master = entityManager.find( ManyToOneNotInsertableEntity.class, mto_id1 );
					master.setNumber( type_id2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( NotInsertableEntityType.class, type_id1 ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( NotInsertableEntityType.class, type_id2 ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( ManyToOneNotInsertableEntity.class, mto_id1 ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testNotInsertableEntity() {
		ManyToOneNotInsertableEntity ver1 = getAuditReader().find( ManyToOneNotInsertableEntity.class, mto_id1, 1 );
		ManyToOneNotInsertableEntity ver2 = getAuditReader().find( ManyToOneNotInsertableEntity.class, mto_id1, 2 );
		ManyToOneNotInsertableEntity ver3 = getAuditReader().find( ManyToOneNotInsertableEntity.class, mto_id1, 3 );

		inTransaction(
				entityManager -> {
					final NotInsertableEntityType type1 = entityManager.find( NotInsertableEntityType.class, type_id1 );
					final NotInsertableEntityType type2 = entityManager.find( NotInsertableEntityType.class, type_id2 );

					assertThat( ver1, nullValue() );
					assertThat( ver2.getType(), equalTo( type1 ) );
					assertThat( ver3.getType(), equalTo( type2 ) );
				}
		);
	}
}
