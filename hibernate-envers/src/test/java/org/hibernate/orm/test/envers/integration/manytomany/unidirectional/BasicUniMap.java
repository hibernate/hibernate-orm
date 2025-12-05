/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.Arrays;
import java.util.HashMap;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.MapUniEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, MapUniEntity.class})
public class BasicUniMap {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StrTestEntity str1 = new StrTestEntity( "str1" );
		StrTestEntity str2 = new StrTestEntity( "str2" );

		MapUniEntity coll1 = new MapUniEntity( 3, "coll1" );

		// Revision 1 (coll1: initialy one mapping)
		scope.inTransaction( em -> {
			em.persist( str1 );
			em.persist( str2 );

			coll1.setMap( new HashMap<String, StrTestEntity>() );
			coll1.getMap().put( "1", str1 );
			em.persist( coll1 );
		} );

		// Revision 2 (coll1: adding one mapping)
		scope.inTransaction( em -> {
			StrTestEntity str2Ref = em.find( StrTestEntity.class, str2.getId() );
			MapUniEntity coll1Ref = em.find( MapUniEntity.class, coll1.getId() );

			coll1Ref.getMap().put( "2", str2Ref );
		} );

		// Revision 3 (coll1: replacing one mapping)
		scope.inTransaction( em -> {
			StrTestEntity str1Ref = em.find( StrTestEntity.class, str1.getId() );
			MapUniEntity coll1Ref = em.find( MapUniEntity.class, coll1.getId() );

			coll1Ref.getMap().put( "2", str1Ref );
		} );

		// Revision 4 (coll1: removing one mapping)
		scope.inTransaction( em -> {
			MapUniEntity coll1Ref = em.find( MapUniEntity.class, coll1.getId() );

			coll1Ref.getMap().remove( "1" );
		} );

		str1_id = str1.getId();
		str2_id = str2.getId();

		coll1_id = coll1.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( MapUniEntity.class, coll1_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str2_id ) );
		} );
	}

	@Test
	public void testHistoryOfColl1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity str1 = em.find( StrTestEntity.class, str1_id );
			StrTestEntity str2 = em.find( StrTestEntity.class, str2_id );

			MapUniEntity rev1 = auditReader.find( MapUniEntity.class, coll1_id, 1 );
			MapUniEntity rev2 = auditReader.find( MapUniEntity.class, coll1_id, 2 );
			MapUniEntity rev3 = auditReader.find( MapUniEntity.class, coll1_id, 3 );
			MapUniEntity rev4 = auditReader.find( MapUniEntity.class, coll1_id, 4 );

			assert rev1.getMap().equals( TestTools.makeMap( "1", str1 ) );
			assert rev2.getMap().equals( TestTools.makeMap( "1", str1, "2", str2 ) );
			assert rev3.getMap().equals( TestTools.makeMap( "1", str1, "2", str1 ) );
			assert rev4.getMap().equals( TestTools.makeMap( "2", str1 ) );

			assert "coll1".equals( rev1.getData() );
			assert "coll1".equals( rev2.getData() );
			assert "coll1".equals( rev3.getData() );
			assert "coll1".equals( rev4.getData() );
		} );
	}
}
