/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.test.type.StringifiedCollectionTypeContributor.LongList;
import org.hibernate.testing.TestForIssue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContributorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				SpecialItem.class,
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.GENERATE_STATISTICS, "true" );

		try {
			ArrayList<ClassLoader> loaders = new ArrayList<>();
			Enumeration<URL> testBases = getClass().getClassLoader().getResources( "org/hibernate/" );
			// simple getResource returns null without trying
			String path = null;
			while ( testBases.hasMoreElements() ) {
				URL testbase = testBases.nextElement();
				String basepath = testbase.toString();
				if ( basepath.contains( "/resources/test/org/hibernate" ) ) {
					path = basepath;
					break;
				}
			}
			if (path == null) {
				// check will always be valid unless gradle is replaced or drastically changed beyond recognition
				throw new IllegalStateException( "Could not find test resources path" );
			}
			path = path + "test/spi/";
			URL url = new URL( path );
			URLClassLoader ucl = new URLClassLoader( new URL[]{ url } );

			loaders.add( ucl );
			options.put( org.hibernate.cfg.AvailableSettings.CLASSLOADERS, loaders );
		}
		catch (java.io.IOException mue) {
			throw new IllegalStateException( mue );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11409")
	public void testParameterRegisterredCollection() {
		final EntityManager em  = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			{
				SpecialItem item = new SpecialItem( "LongList", new LongList(5L, 11L, 6123L, -61235L, 24L) );
				em.persist( item );
				// deallocate item after braces close
			}
			em.getTransaction().commit();

			em.getTransaction().begin();
			LongList ll = new LongList();
			ll.add( 5L );
			ll.add( 11L );
			ll.add( 6123L );
			ll.add( -61235L );
			ll.add( 24L );
			// test native, because JPQL query isn't affected by the issue
			Query tq = em.createNativeQuery( "SELECT * FROM special_table WHERE longvals = ?", SpecialItem.class );
			tq.setParameter(1, ll);
			SpecialItem item = (SpecialItem) tq.getSingleResult();
			em.getTransaction().commit();
			assertEquals( "LongList", item.getName() );
			item = null;
			tq = null;
			em.clear();

			em.getTransaction().begin();
			ll = new LongList();
			ll.add( 5L );
			ll.add( 11L );
			ll.add( 6123L );
			ll.add( -61235L );
			ll.add( 24L );
			// Test JPQL as well, just in case
			tq = em.createQuery( "FROM SpecialItem WHERE longvals = :itm", SpecialItem.class );
			tq.setParameter("itm", ll);
			item = (SpecialItem) tq.getSingleResult();
			em.getTransaction().commit();
			assertEquals( "LongList", item.getName() );
		}
		catch (Exception e) {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Entity(name = "SpecialItem")
	@Table(name = "special_table")
	public static class SpecialItem implements Serializable {

		@Id
		private String name;
		@Column(columnDefinition = "text")
		private LongList longvals;

		public SpecialItem() {
		}

		public SpecialItem(String name, LongList longvals) {
			this.name = name;
			this.longvals = longvals;
		}

		public LongList getLongvals() {
			return longvals;
		}

		public void setLongvals(LongList longvals) {
			this.longvals = longvals;
		}

		@Id
		@Column(length = 30)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
