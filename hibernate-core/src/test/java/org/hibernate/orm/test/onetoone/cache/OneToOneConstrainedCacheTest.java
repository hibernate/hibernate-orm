/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.cache;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Simple testcase to illustrate HB-992
 *
 * @author Wolfgang Voelkl, michael
 */
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/onetoone/cache/Object2.hbm.xml", "org/hibernate/orm/test/onetoone/cache/MainObject.hbm.xml" }
)
@SessionFactory
public class OneToOneConstrainedCacheTest {

	@Test
	public void testOneToOneCache(SessionFactoryScope scope) {

		//create a new MainObject
		Object mainObjectId = createMainObject( scope );
		// load the MainObject
		readMainObject( mainObjectId, scope );

		//create and add Ojbect2
		addObject2( mainObjectId, scope );

		//here the newly created Object2 is written to the database
		//but the MainObject does not know it yet
		MainObject mainObject = readMainObject( mainObjectId, scope );

		assertNotNull( mainObject.getObj2() );

		// after evicting, it works.
		scope.getSessionFactory().getCache().evictEntityData( MainObject.class );

		mainObject = readMainObject( mainObjectId, scope );

		assertNotNull( mainObject.getObj2() );
	}

	/**
	 * creates a new MainObject
	 * <p>
	 * one hibernate transaction !
	 */
	private Object createMainObject(SessionFactoryScope scope) {
		MainObject mainObject = scope.fromTransaction(
				session -> {
					MainObject mo = new MainObject();
					mo.setDescription( "Main Test" );

					session.persist( mo );
					return mo;
				}
		);
		return mainObject.getId();
	}

	/**
	 * loads the newly created MainObject
	 * and adds a new Object2 to it
	 * <p>
	 * one hibernate transaction
	 */
	private void addObject2(Object mainObjectId, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MainObject mo = session.getReference( MainObject.class, mainObjectId );

					Object2 toAdd = new Object2();
					toAdd.setDummy( "test" );

					mo.setObj2( toAdd );
				}
		);
	}

	/**
	 * reads the newly created MainObject
	 * and its Object2 if it exists
	 * <p>
	 * one hibernate transaction
	 */
	private MainObject readMainObject(Object id, SessionFactoryScope scope) {
		return scope.fromTransaction(
				session ->
						session.get( MainObject.class, id )
		);
	}
}
