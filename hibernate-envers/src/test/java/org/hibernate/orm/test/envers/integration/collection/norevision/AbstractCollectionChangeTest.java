/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.norevision;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractCollectionChangeTest {
	protected Integer personId;
	protected Integer parentId;
	protected Integer houseId;

	protected abstract List<Integer> getExpectedPersonRevisions();

	protected abstract List<Integer> getExpectedParentRevisions();

	protected abstract List<Integer> getExpectedHouseRevisions();

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Rev 1
		scope.inTransaction( em -> {
			Person p = new Person();
			Name n = new Name();
			n.setName( "name1" );
			p.getNames().add( n );
			em.persist( p );
			personId = p.getId();
		} );

		// Rev 2
		scope.inTransaction( em -> {
			Person p = em.find( Person.class, personId );
			Name n = p.getNames().iterator().next();
			n.setName( "Changed name" );
			em.merge( p );
		} );

		// Rev 3
		scope.inTransaction( em -> {
			Person p = em.find( Person.class, personId );
			Name n2 = new Name();
			n2.setName( "name2" );
			p.getNames().add( n2 );
		} );

		// Rev 4
		scope.inTransaction( em -> {
			Child child1 = new Child();
			Parent parent = new Parent();
			parent.setName( "P1" );
			child1.setParent( parent );
			parent.getChildren().add( child1 );
			em.persist( child1 );
			em.persist( parent );
			parentId = parent.getId();
		} );

		// Rev 5
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, parentId );
			Child child2 = new Child();
			parent.getChildren().add( child2 );
			child2.setParent( parent );
			em.persist( child2 );
			em.persist( parent );
		} );

		// Rev 6
		scope.inTransaction( em -> {
			House house = new House();
			house.getColors().add( "Red" );
			em.persist( house );
			houseId = house.getId();
		} );

		// Rev 7
		scope.inTransaction( em -> {
			House house = em.find( House.class, houseId );
			house.getColors().add( "Blue" );
			em.merge( house );
		} );
	}

	@Test
	public void testPersonRevisionCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( getExpectedPersonRevisions(),
					auditReader.getRevisions( Person.class, personId ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10201")
	public void testParentRevisionCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( getExpectedParentRevisions(),
					auditReader.getRevisions( Parent.class, parentId ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10201")
	public void testHouseRevisionCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( getExpectedHouseRevisions(),
					auditReader.getRevisions( House.class, houseId ) );
		} );
	}
}
