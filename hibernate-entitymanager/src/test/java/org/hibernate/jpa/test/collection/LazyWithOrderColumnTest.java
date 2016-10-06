/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.jpa.test.collection;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Yoann Rodière
 */
public class LazyWithOrderColumnTest extends BaseEntityManagerFunctionalTestCase {

	/*
	 * Just to show that insertion works fine with an initialized proxy.
	 * See test for issue HHH-8083 (testAddElementWithProxyUninitialized below).
	 */
	@Test
	public void testAddElementWithProxyInitialized() {
		doTestAddElement( true /* initialize the proxy before adding */);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8083" )
	public void testAddElementWithProxyUninitialized() {
		doTestAddElement( false /* do not initialize the proxy before adding: triggers the bug in HHH-8083 */);
	}

	private void doTestAddElement(boolean initializeProxyBeforeAdding) {
		EntityManager em = getOrCreateEntityManager();

		Container container = new Container();
		container.getItems().add( new Item( container, "Name" ) );
		container.getItems().add( new Item( container, "Name2" ) );
		em.getTransaction().begin();
		em.persist( container );
		em.flush();
		em.clear();
		em.getTransaction().commit();

		// Test
		em.getTransaction().begin();
		container = em.find( Container.class, container.id );
		if ( initializeProxyBeforeAdding ) {
			container.getItems().size(); // Initializes the proxy
		}
		container.getItems().add( new Item( container, "Name3" ) );
		em.flush();
		em.clear();
		em.getTransaction().commit();

		List<String> expectedContent = Arrays.asList( "Name", "Name2", "Name3" );
		em.getTransaction().begin();
		container = em.find( Container.class, container.id );
		assertEquals( expectedContent, getItemNames( container ) );
		em.getTransaction().rollback();
		em.close();
	}

	private static List<String> getItemNames(Container container) {
		List<String> names = new ArrayList<String>();
		for ( Item item : container.getItems() ) {
			names.add( item.getName() );
		}
		return names;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Container.class, Item.class };
	}
}
