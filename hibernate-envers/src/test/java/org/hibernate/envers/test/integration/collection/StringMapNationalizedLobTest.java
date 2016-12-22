/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class StringMapNationalizedLobTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Simple simple = new Simple( 1, "Simple" );
			simple.getEmbeddedMap().put( "1", "One" );
			simple.getEmbeddedMap().put( "2", "Two" );
			entityManager.persist( simple );
		} );
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().put( "3", "Three" );
			entityManager.merge( simple );
		} );
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().remove( "1" );
			simple.getEmbeddedMap().remove( "2" );
			entityManager.merge( simple );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( Simple.class, 1 ) );
	}

	@Test
	public void testRevisionHistory() {
		final Simple rev1 = getAuditReader().find( Simple.class, 1, 1 );
		assertEquals( 2, rev1.getEmbeddedMap().entrySet().size() );
		assertEquals( TestTools.makeSet( "1", "2" ), rev1.getEmbeddedMap().keySet() );

		final Simple rev2 = getAuditReader().find( Simple.class, 1, 2 );
		assertEquals( 3, rev2.getEmbeddedMap().entrySet().size() );
		assertEquals( TestTools.makeSet( "1", "2", "3" ), rev2.getEmbeddedMap().keySet() );

		final Simple rev3 = getAuditReader().find( Simple.class, 1, 3 );
		assertEquals( 1, rev3.getEmbeddedMap().entrySet().size() );
		assertEquals( TestTools.makeSet( "3" ), rev3.getEmbeddedMap().keySet() );
	}

	@Entity(name = "Simple")
	@Audited
	public static class Simple {
		@Id
		private Integer id;
		private String name;

		@ElementCollection
		@Lob
		@Nationalized
		private Map<String, String> embeddedMap = new HashMap<String, String>();

		Simple() {

		}

		Simple(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Map<String,String> getEmbeddedMap() {
			return embeddedMap;
		}

		public void setEmbeddedMap(Map<String,String> embeddedMap) {
			this.embeddedMap = embeddedMap;
		}
	}
}
