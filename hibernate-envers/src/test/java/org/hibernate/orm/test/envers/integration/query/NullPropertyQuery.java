/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefIngEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefIngEmbIdEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Jpa(annotatedClasses = {
		StrIntTestEntity.class,
		SetRefEdEmbIdEntity.class,
		SetRefIngEmbIdEntity.class,
		CollectionRefEdEntity.class,
		CollectionRefIngEntity.class
})
@EnversTest
public class NullPropertyQuery {
	private Integer idSimplePropertyNull = null;
	private Integer idSimplePropertyNotNull = null;
	private EmbId idMulticolumnReferenceToParentNull = new EmbId( 0, 1 );
	private Integer idReferenceToParentNotNull = 1;
	private Integer idParent = 1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			StrIntTestEntity nullSite = new StrIntTestEntity( null, 1 );
			StrIntTestEntity notNullSite = new StrIntTestEntity( "data", 2 );
			em.persist( nullSite );
			em.persist( notNullSite );
			idSimplePropertyNull = nullSite.getId();
			idSimplePropertyNotNull = notNullSite.getId();
			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();
			SetRefIngEmbIdEntity nullParentSrieie = new SetRefIngEmbIdEntity(
					idMulticolumnReferenceToParentNull,
					"data",
					null
			);
			em.persist( nullParentSrieie );
			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();
			CollectionRefEdEntity parent = new CollectionRefEdEntity( idParent, "data" );
			CollectionRefIngEntity notNullParentCrie = new CollectionRefIngEntity(
					idReferenceToParentNotNull,
					"data",
					parent
			);
			em.persist( parent );
			em.persist( notNullParentCrie );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testSimplePropertyIsNullQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity ver = (StrIntTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
					.add( AuditEntity.property( "str1" ).isNull() )
					.getSingleResult();

			assertEquals( new StrIntTestEntity( null, 1, idSimplePropertyNull ), ver );
		} );
	}

	@Test
	public void testSimplePropertyIsNotNullQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity ver = (StrIntTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
					.add( AuditEntity.property( "str1" ).isNotNull() )
					.getSingleResult();

			assertEquals( new StrIntTestEntity( "data", 2, idSimplePropertyNotNull ), ver );
		} );
	}

	@Test
	public void testReferenceMulticolumnPropertyIsNullQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			SetRefIngEmbIdEntity ver = (SetRefIngEmbIdEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
					.add( AuditEntity.property( "reference" ).isNull() )
					.getSingleResult();

			assertEquals( idMulticolumnReferenceToParentNull, ver.getId() );
		} );
	}

	@Test
	public void testReferencePropertyIsNotNullQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			CollectionRefIngEntity ver = (CollectionRefIngEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( CollectionRefIngEntity.class, 3 )
					.add( AuditEntity.property( "reference" ).isNotNull() )
					.getSingleResult();

			assertEquals( idReferenceToParentNotNull, ver.getId() );
		} );
	}
}
