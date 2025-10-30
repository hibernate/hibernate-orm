/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.inverseToSuperclass;

import java.util.ArrayList;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Hern�n Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/manyToMany/inverseToSuperclass/mappings.hbm.xml")
public class ManyToManyInverseToSuperclassTest {
	private long m1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Root m1 = new Root();
			DetailSubclass det1 = new DetailSubclass2();

			// Revision 1
			det1.setStr2( "detail 1" );

			m1.setStr( "root" );
			m1.setItems( new ArrayList<DetailSubclass>() );
			m1.getItems().add( det1 );

			det1.setRoots( new ArrayList<Root>() );
			det1.getRoots().add( m1 );

			em.persist( m1 );
			m1_id = m1.getId();
		} );
	}

	@Test
	public void testHistoryExists(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
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
