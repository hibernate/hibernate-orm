/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefIngEntity;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefIngEmbIdEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NullPropertyQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer idSimplePropertyNull = null;
	private Integer idSimplePropertyNotNull = null;
	private EmbId idMulticolumnReferenceToParentNull = new EmbId( 0, 1 );
	private Integer idReferenceToParentNotNull = 1;
	private Integer idParent = 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				StrIntTestEntity.class,
				SetRefEdEmbIdEntity.class,
				SetRefIngEmbIdEntity.class,
				CollectionRefEdEntity.class,
				CollectionRefIngEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					StrIntTestEntity nullSite = new StrIntTestEntity( null, 1 );
					StrIntTestEntity notNullSite = new StrIntTestEntity( "data", 2 );
					entityManager.persist( nullSite );
					entityManager.persist( notNullSite );
					idSimplePropertyNull = nullSite.getId();
					idSimplePropertyNotNull = notNullSite.getId();
				},

				// Revision 2
				entityManager -> {
					SetRefIngEmbIdEntity nullParentSrieie = new SetRefIngEmbIdEntity(
							idMulticolumnReferenceToParentNull,
							"data",
							null
					);
					entityManager.persist( nullParentSrieie );
				},

				// Revision 3
				entityManager -> {
					CollectionRefEdEntity parent = new CollectionRefEdEntity( idParent, "data" );
					CollectionRefIngEntity notNullParentCrie = new CollectionRefIngEntity(
							idReferenceToParentNotNull,
							"data",
							parent
					);
					entityManager.persist( parent );
					entityManager.persist( notNullParentCrie );
				}
		);
	}

	@DynamicTest
	public void testSimplePropertyIsNullQuery() {
		StrIntTestEntity ver = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "str1" ).isNull() )
				.getSingleResult();
		assertThat( ver, equalTo( new StrIntTestEntity( null, 1, idSimplePropertyNull ) ) );
	}

	@DynamicTest
	public void testSimplePropertyIsNotNullQuery() {
		StrIntTestEntity ver = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "str1" ).isNotNull() )
				.getSingleResult();
		assertThat( ver, equalTo( new StrIntTestEntity( "data", 2, idSimplePropertyNotNull ) ) );
	}

	@DynamicTest
	public void testReferenceMulticolumnPropertyIsNullQuery() {
		SetRefIngEmbIdEntity ver = (SetRefIngEmbIdEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
				.add( AuditEntity.property( "reference" ).isNull() )
				.getSingleResult();
		assertThat( ver.getId(), equalTo( idMulticolumnReferenceToParentNull ) );
	}

	@DynamicTest
	public void testReferencePropertyIsNotNullQuery() {
		CollectionRefIngEntity ver = (CollectionRefIngEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( CollectionRefIngEntity.class, 3 )
				.add( AuditEntity.property( "reference" ).isNotNull() )
				.getSingleResult();
		assertThat( ver.getId(), equalTo( idReferenceToParentNotNull ) );
	}
}