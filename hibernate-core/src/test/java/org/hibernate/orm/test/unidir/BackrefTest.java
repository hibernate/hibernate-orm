/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unidir;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/unidir/ParentChild.hbm.xml"
//		annotatedClasses = { Parent1.class, Child1.class, Child2.class }
)
@SessionFactory
public class BackrefTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testBackRef(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Parent p = new Parent("Marc", 123456789);
			Parent p2 = new Parent("Nathalie", 987654321 );
			Child c = new Child("Elvira");
			Child c2 = new Child("Blase");
			p.getChildren().add(c);
			p.getChildren().add(c2);
			session.persist(p);
			session.persist(p2);
		} );

		factoryScope.inTransaction( (s) -> {
			var c = s.find( Child.class, "Elvira" );
			c.setAge(2);
		} );

		factoryScope.inTransaction( (s) -> {
			var c = s.find( Child.class, "Elvira" );
			c.setAge(18);
		} );

		factoryScope.inTransaction( (s) -> {
			var p = s.find( Parent.class, "Marc" );
			var p2 = s.find( Parent.class, "Nathalie" );
			var c = s.find( Child.class, "Elvira" );
			Assertions.assertEquals( 0, p.getChildren().indexOf(c) );
			p.getChildren().remove(c);
			p2.getChildren().add(c);
		} );

		factoryScope.inTransaction( (s) -> {
			var p3 = new Parent("Marion", 543216789);
			p3.getChildren().add( new Child("Gavin") );
			s.merge(p3);
		} );
	}

	@Test
	public void testBackRefToProxiedEntityOnMerge(SessionFactoryScope factoryScope) {
		var me = factoryScope.fromTransaction( (s) -> {
			var steve = new Parent( "Steve", 192837465 );
			steve.getChildren().add( new Child( "Joe" ) );
			s.persist( steve );
			return steve;
		} );

		// while detached, add a new element
		me.getChildren().add( new Child( "Cece" ) );
		me.getChildren().add( new Child( "Austin" ) );

		// load 'me' to associate it with the new session as a proxy (this may have occurred as 'prior work'
		// to the reattachment below)...
		factoryScope.inTransaction( (s) -> {
			Object meProxy = s.getReference( Parent.class, me.getName() );
			Assertions.assertFalse( Hibernate.isInitialized( meProxy ) );
			// now, do the reattchment...
			s.merge( me );
		} );
	}
}
