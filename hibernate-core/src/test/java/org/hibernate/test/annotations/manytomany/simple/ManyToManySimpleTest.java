/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.manytomany.simple;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner (extracted from ManyToManyTest authored by Emmanuel Bernard)
 */
@FailureExpectedWithNewMetamodel
public class ManyToManySimpleTest extends BaseCoreFunctionalTestCase {
	@Test
	 public void testDefault() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		KnownClient emmanuel = new KnownClient();
		emmanuel.setName( "Emmanuel" );
		emmanuel.setStores( new HashSet<Store>() );
		fnac.setCustomers( new HashSet<KnownClient>() );
		fnac.getCustomers().add( emmanuel );
		emmanuel.getStores().add( fnac );
		fnac.setImplantedIn( new HashSet<City>() );
		City paris = new City();
		fnac.getImplantedIn().add( paris );
		paris.setName( "Paris" );
		s.persist( fnac );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Store store;
		KnownClient knownClient;
		City city;
		store = (Store) s.get( Store.class, fnac.getId() );
		assertNotNull( store );
		assertNotNull( store.getCustomers() );
		assertEquals( 1, store.getCustomers().size() );
		knownClient = store.getCustomers().iterator().next();
		assertEquals( emmanuel.getName(), knownClient.getName() );
		assertNotNull( store.getImplantedIn() );
		assertEquals( 1, store.getImplantedIn().size() );
		city = store.getImplantedIn().iterator().next();
		assertEquals( paris.getName(), city.getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		knownClient = (KnownClient) s.get( KnownClient.class, emmanuel.getId() );
		assertNotNull( knownClient );
		assertNotNull( knownClient.getStores() );
		assertEquals( 1, knownClient.getStores().size() );
		store = knownClient.getStores().iterator().next();
		assertEquals( fnac.getName(), store.getName() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCanUseCriteriaQuery() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		Supplier emi = new Supplier();
		emi.setName( "Emmanuel" );
		emi.setSuppStores( new HashSet<Store>() );
		fnac.setSuppliers( new HashSet<Supplier>() );
		fnac.getSuppliers().add( emi );
		emi.getSuppStores().add( fnac );
		s.persist( fnac );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		List result = s.createCriteria( Supplier.class ).createAlias( "suppStores", "s" ).add(
				Restrictions.eq( "s.name", "Fnac" ) ).list();
		assertEquals( 1, result.size() );
		tx.commit();
		s.close();
	}

	@Test
	public void testMappedBy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		Supplier emi = new Supplier();
		emi.setName( "Emmanuel" );
		emi.setSuppStores( new HashSet<Store>() );
		fnac.setSuppliers( new HashSet<Supplier>() );
		fnac.getSuppliers().add( emi );
		emi.getSuppStores().add( fnac );
		s.persist( fnac );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Store store;
		Supplier supplier;
		store = (Store) s.get( Store.class, fnac.getId() );
		assertNotNull( store );
		assertNotNull( store.getSuppliers() );
		assertEquals( 1, store.getSuppliers().size() );
		supplier = store.getSuppliers().iterator().next();
		assertEquals( emi.getName(), supplier.getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		supplier = (Supplier) s.get( Supplier.class, emi.getId() );
		assertNotNull( supplier );
		assertNotNull( supplier.getSuppStores() );
		assertEquals( 1, supplier.getSuppStores().size() );
		store = supplier.getSuppStores().iterator().next();
		assertEquals( fnac.getName(), store.getName() );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Store.class,
				KnownClient.class,
				Supplier.class,
				City.class,
		};
	}
}
