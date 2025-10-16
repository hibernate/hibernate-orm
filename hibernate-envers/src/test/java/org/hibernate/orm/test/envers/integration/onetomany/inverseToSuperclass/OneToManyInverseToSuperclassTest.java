/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.inverseToSuperclass;

import java.util.ArrayList;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/oneToMany/inverseToSuperclass/mappings.hbm.xml")
public class OneToManyInverseToSuperclassTest {

	private long m1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1
			Root m1 = new Root();
			DetailSubclass det1 = new DetailSubclass2();

			det1.setStr2( "detail 1" );

			m1.setStr( "root" );
			m1.setItems( new ArrayList<DetailSubclass>() );
			m1.getItems().add( det1 );
			det1.setParent( m1 );

			em.persist( m1 );

			m1_id = m1.getId();
		} );

		scope.inTransaction( em -> {
			// Revision 2
			Root m1 = em.find( Root.class, m1_id );
			DetailSubclass det2 = new DetailSubclass2();

			det2.setStr2( "detail 2" );
			det2.setParent( m1 );
			m1.getItems().add( det2 );
		} );

		scope.inTransaction( em -> {
			// Revision 3
			Root m1 = em.find( Root.class, m1_id );
			m1.setStr( "new root" );

			DetailSubclass det1 = m1.getItems().get( 0 );
			det1.setStr2( "new detail" );
			DetailSubclass det3 = new DetailSubclass2();
			det3.setStr2( "detail 3" );
			det3.setParent( m1 );

			m1.getItems().get( 1 ).setParent( null );
			m1.getItems().add( det3 );

			em.persist( m1 );
		} );

		scope.inTransaction( em -> {
			// Revision 4
			Root m1 = em.find( Root.class, m1_id );

			DetailSubclass det1 = m1.getItems().get( 0 );
			det1.setParent( null );

			em.persist( m1 );
		} );
	}

	@Test
	public void testHistoryExists(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Root rev1_1 = auditReader.find( Root.class, m1_id, 1 );
			Root rev1_2 = auditReader.find( Root.class, m1_id, 2 );
			Root rev1_3 = auditReader.find( Root.class, m1_id, 3 );
			Root rev1_4 = auditReader.find( Root.class, m1_id, 4 );

			assertNotNull( rev1_1 );
			assertNotNull( rev1_2 );
			assertNotNull( rev1_3 );
			assertNotNull( rev1_4 );
		} );
	}
}
