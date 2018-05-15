/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.custom.declaredtype.explicitsemantics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Max Rydahl Andersen
 * @author David Weinberg
 */
public class UserCollectionTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ User.class, Email.class };
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	public void testBasicOperation() {
		doInHibernate( this::sessionFactory, session -> {
			User u = new User( "max" );
			u.getEmailAddresses().add( new Email( "max@hibernate.org" ) );
			u.getEmailAddresses().add( new Email( "max.andersen@jboss.com" ) );
			session.persist( u );
		} );

		doInHibernate( this::sessionFactory, session -> {
			User u2 = (User) session.createCriteria( User.class ).uniqueResult();
			assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
			assertEquals( u2.getEmailAddresses().size(), 2 );
			assertNotNull( u2.getEmailAddresses().head() );
		} );
	}

	/**
	 * @author Gavin King
	 * @author Steve Ebersole
	 */
	@Entity(name = "Email")
	public static class Email {

		private Long id;
		private String address;

		Email() {
		}

		public Email(String address) {
			this.address = address;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long getId() {
			return id;
		}

		private void setId(Long id) {
			this.id = id;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String type) {
			this.address = type;
		}

		@Override
		public boolean equals(Object that) {
			if ( !( that instanceof Email ) )
				return false;
			Email p = (Email) that;
			return this.address.equals( p.address );
		}

		@Override
		public int hashCode() {
			return address.hashCode();
		}

	}

	@Entity(name = "User")
	@Table(name = "UC_BSC_USER")
	@TypeDef(name = "HeadSetListType", typeClass = HeadSetListType.class)
	public static class User {

		private String userName;

		private IHeadSetList<Email> emailAddresses = new HeadSetList<Email>();

		private Map sessionData = new HashMap();

		User() {

		}

		public User(String name) {
			userName = name;
		}

		@Id
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
		@CollectionType(type = "HeadSetListType", semantics = Set.class)
		@JoinColumn(name = "userName")
		@OrderColumn(name = "displayOrder")
		public IHeadSetList<Email> getEmailAddresses() { // can declare a custom interface type
			return emailAddresses;
		}

		public void setEmailAddresses(IHeadSetList<Email> emailAddresses) {
			this.emailAddresses = emailAddresses;
		}

		@Transient
		public Map getSessionData() {
			return sessionData;
		}

		public void setSessionData(Map sessionData) {
			this.sessionData = sessionData;
		}
	}

	public static class HeadSetListType implements UserCollectionType {

		@Override
		public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
				throws HibernateException {
			return new PersistentHeadList( session );
		}

		@Override
		public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
			return new PersistentHeadList( session, (IHeadSetList) collection );
		}

		@Override
		public Iterator getElementsIterator(Object collection) {
			return ( (IHeadSetList) collection ).iterator();
		}

		@Override
		public boolean contains(Object collection, Object entity) {
			return ( (IHeadSetList) collection ).contains( entity );
		}

		@Override
		public Object indexOf(Object collection, Object entity) {
			int l = ( (IHeadSetList) collection ).indexOf( entity );
			if ( l < 0 ) {
				return null;
			}
			else {
				return l;
			}
		}

		@Override
		public Object replaceElements(
				Object original,
				Object target,
				CollectionPersister persister,
				Object owner,
				Map copyCache,
				SharedSessionContractImplementor session) throws HibernateException {
			IHeadSetList result = (IHeadSetList) target;
			result.clear();
			result.addAll( (HeadSetList) original );
			return result;
		}

		@Override
		public Object instantiate(int anticipatedSize) {
			return new HeadSetList();
		}
	}

	public interface IHeadSetList<X> extends Set<X>, List<X> {

		X head();

		@Override
		default Spliterator<X> spliterator() {
			return Spliterators.spliterator( this, Spliterator.DISTINCT );
		}
	}

	/**
	 * A custom collection class that has both List and Set interfaces, but only really implements set for persistence
	 * (e.g. extends PersistentSet). Without setting the semantics on the CollectionType annotation, List semantics
	 * would be inferred, and that would not match the implemented methods in PersistentSet and would fail. HeadSetList
	 * is very much a toy collection type.
	 *
	 * @author David Weinberg
	 */
	public static class HeadSetList<X> extends ArrayList<X> implements IHeadSetList<X> {

		@Override
		public X head() {
			return isEmpty() ? null : get( 0 );
		}
	}

	public static class PersistentHeadList extends PersistentSet implements IHeadSetList {

		public PersistentHeadList(SharedSessionContractImplementor session) {
			super( session );
		}

		public PersistentHeadList(SharedSessionContractImplementor session, IHeadSetList list) {
			super( session, list );
		}

		@Override
		public Object head() {
			return ( (IHeadSetList) set ).head();
		}

		@Override
		public boolean addAll(int index, Collection c) {
			return set.addAll( c );
		}

		@Override
		public Object get(int index) {
			Iterator iterator = iterator();
			Object next = null;
			for ( int i = 0; i <= index; i++ ) {
				next = iterator.next();
			}
			return next;
		}

		@Override
		public Object set(int index, Object element) {
			remove( index );
			return add( element );
		}

		@Override
		public void add(int index, Object element) {
			add( element );
		}

		@Override
		public Object remove(int index) {
			return remove( get( index ) );
		}

		@Override
		public int indexOf(Object o) {
			throw new UnsupportedOperationException( "Toy class" );
		}

		@Override
		public int lastIndexOf(Object o) {
			throw new UnsupportedOperationException( "Toy class" );
		}

		@Override
		public ListIterator listIterator() {
			throw new UnsupportedOperationException( "Toy class" );
		}

		@Override
		public ListIterator listIterator(int index) {
			throw new UnsupportedOperationException( "Toy class" );
		}

		@Override
		public List subList(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException( "Toy class" );
		}
	}
}
