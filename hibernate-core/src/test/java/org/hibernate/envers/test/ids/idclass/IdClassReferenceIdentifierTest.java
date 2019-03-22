/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids.idclass;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.idclass.ClassType;
import org.hibernate.envers.test.support.domains.ids.idclass.IntegerGeneratedIdentityEntity;
import org.hibernate.envers.test.support.domains.ids.idclass.ReferenceIdentifierClassId;
import org.hibernate.envers.test.support.domains.ids.idclass.ReferenceIdentifierEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Matthew Morrissette (yinzara at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-10667")
@Disabled("NYI - @IdClass support")
public class IdClassReferenceIdentifierTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private ReferenceIdentifierClassId entityId = null;
	private Integer typeId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ReferenceIdentifierEntity.class,
				ReferenceIdentifierClassId.class,
				ClassType.class,
				IntegerGeneratedIdentityEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final ClassType type = new ClassType( "type", "initial description" );
					entityManager.persist( type );

					final IntegerGeneratedIdentityEntity type2 = new IntegerGeneratedIdentityEntity();
					entityManager.persist(type2);

					final ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity();
					entity.setSampleValue( "initial data" );
					entity.setType( type );
					entity.setIiie( type2 );
					entityManager.persist( entity );

					this.typeId = type2.getId();
					entityId = new ReferenceIdentifierClassId( typeId, type.getType() );
				},

				// Revision 2
				entityManager -> {
					final ClassType type = entityManager.find( ClassType.class, "type" );
					type.setDescription( "modified description" );
					entityManager.merge( type );
				},

				// Revision 3
				entityManager -> {
					final ReferenceIdentifierEntity entity = entityManager.find( ReferenceIdentifierEntity.class, entityId );
					entity.setSampleValue( "modified data" );
					entityManager.merge( entity );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ClassType.class, "type" ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( IntegerGeneratedIdentityEntity.class, typeId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( ReferenceIdentifierEntity.class, entityId ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		final ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity(
				new IntegerGeneratedIdentityEntity( typeId ),
				new ClassType( "type", "initial description" ),
				"initial data"
		);

		final ReferenceIdentifierEntity ver1 = getAuditReader().find( ReferenceIdentifierEntity.class, entityId, 1 );
		assertThat( ver1.getIiie().getId(), equalTo( entity.getIiie().getId() ) );
		assertThat( ver1.getSampleValue(), equalTo( entity.getSampleValue() ) );
		assertThat( ver1.getType().getType(), equalTo( entity.getType().getType() ) );
		assertThat( ver1.getType().getDescription(), equalTo( entity.getType().getDescription() ) );
	}

	@DynamicTest
	public void testHistoryOfEntity3() {
		final ReferenceIdentifierEntity entity = new ReferenceIdentifierEntity(
				new IntegerGeneratedIdentityEntity( typeId ),
				new ClassType( "type", "modified description" ),
				"modified data"
		);

		final ReferenceIdentifierEntity ver2 = getAuditReader().find( ReferenceIdentifierEntity.class, entityId, 3 );
		assertThat( ver2.getIiie().getId(), equalTo( entity.getIiie().getId() ) );
		assertThat( ver2.getSampleValue(), equalTo( entity.getSampleValue() ) );
		assertThat( ver2.getType().getType(), equalTo( entity.getType().getType() ) );
		assertThat( ver2.getType().getDescription(), equalTo( entity.getType().getDescription() ) );
	}
}
