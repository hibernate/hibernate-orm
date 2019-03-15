/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedAssignedIdEntity;
import org.hibernate.envers.test.support.domains.basic.BasicNonAuditedAssignedIdEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedAPITest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicAuditedAssignedIdEntity.class, BasicNonAuditedAssignedIdEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					BasicAuditedAssignedIdEntity ent1 = new BasicAuditedAssignedIdEntity( 1, "str1" );
					BasicNonAuditedAssignedIdEntity ent2 = new BasicNonAuditedAssignedIdEntity( 1, "str1" );

					entityManager.persist( ent1 );
					entityManager.persist( ent2 );
				},

				entityManager -> {
					BasicAuditedAssignedIdEntity ent1 = entityManager.find( BasicAuditedAssignedIdEntity.class, 1 );
					BasicNonAuditedAssignedIdEntity ent2 = entityManager.find( BasicNonAuditedAssignedIdEntity.class, 1 );
					ent1.setStr1( "str2" );
					ent2.setStr1( "str2" );
				}
		);
	}

	@DynamicTest
	public void testHasChangedCriteria() {
		@SuppressWarnings("unchecked")
		final List<BasicAuditedAssignedIdEntity> entitiesChanged =
				(List<BasicAuditedAssignedIdEntity>) getAuditReader()
						.createQuery()
						.forRevisionsOfEntity( BasicAuditedAssignedIdEntity.class, true, true )
						.add( AuditEntity.property( "str1" ).hasChanged() )
						.getResultList();

		assertThat( entitiesChanged, CollectionMatchers.hasSize( 2 ) );
		assertThat( collectStrings( entitiesChanged ), contains( "str1", "str2" ) );
	}

	@DynamicTest
	public void testHasNotChangedCriteria() {
		@SuppressWarnings("unchecked")
		final List<BasicAuditedAssignedIdEntity> entitiesNotChanged =
				(List<BasicAuditedAssignedIdEntity>) getAuditReader()
						.createQuery()
						.forRevisionsOfEntity( BasicAuditedAssignedIdEntity.class, true, true )
						.add( AuditEntity.property( "str1" ).hasNotChanged() )
						.getResultList();

		assertThat( entitiesNotChanged, CollectionMatchers.isEmpty() );
	}

	private static List<String> collectStrings(List<BasicAuditedAssignedIdEntity> entities) {
		return entities.stream().map( BasicAuditedAssignedIdEntity::getStr1 ).collect( Collectors.toList() );
	}
}
