/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedNullPropertiesTest extends AbstractModifiedFlagsEntityTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final BasicAuditedEntity entity = new BasicAuditedEntity( "x", 1 );
					entityManager.persist( entity );
					this.id1 = entity.getId();
				},

				// Revision 2
				entityManager -> {
					final BasicAuditedEntity entity = new BasicAuditedEntity( null, 20 );
					entityManager.persist( entity );
					this.id2 = entity.getId();
				},

				// Revision 3
				entityManager -> {
					final BasicAuditedEntity entity = entityManager.find( BasicAuditedEntity.class, id1 );
					entity.setLong1( 1 );
					entity.setStr1( null );
					entityManager.merge( entity );
				},

				// Revision 4
				entityManager -> {
					final BasicAuditedEntity entity = entityManager.find( BasicAuditedEntity.class, id2 );
					entity.setLong1( 20 );
					entity.setStr1( "y2" );
					entityManager.merge( entity );
				}
		);
	}

	@DynamicTest
	public void testHasChanged() {
		final List str1Changes1 = queryForPropertyHasChangedWithDeleted( BasicAuditedEntity.class, id1, "str1" );
		assertThat( extractRevisions( str1Changes1 ), contains( 1, 3 ) );

		final List long1Changes1 = queryForPropertyHasChangedWithDeleted( BasicAuditedEntity.class, id1, "long1" );
		assertThat( extractRevisions(long1Changes1 ), contains( 1 ) );

		// str1 property was null before insert and after insert so in a way it did not change.
		// is it a good way to go?
		final List str1Changes2 = queryForPropertyHasChangedWithDeleted( BasicAuditedEntity.class, id2, "str1" );
		assertThat( extractRevisions( str1Changes2 ), contains( 4 ) );

		final List long1Changes2 = queryForPropertyHasChangedWithDeleted( BasicAuditedEntity.class, id2, "long1" );
		assertThat( extractRevisions( long1Changes2 ), contains( 2 ) );

		final List revisions = getAuditReader().createQuery()
				.forRevisionsOfEntity( BasicAuditedEntity.class, false, true )
				.add( AuditEntity.property( "str1" ).hasChanged() )
				.add( AuditEntity.property( "long1" ).hasChanged() )
				.getResultList();
		assertThat( extractRevisions( revisions ), contains( 1 ) );
	}
}