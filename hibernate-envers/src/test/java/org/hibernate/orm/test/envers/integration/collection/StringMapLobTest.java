/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-9834")
@SkipForDialect(OracleDialect.class)
@SkipForDialect(value = PostgreSQLDialect.class, jiraKey = "HHH-11477", comment = "@Lob field in HQL predicate fails with error about text = bigint")
@SkipForDialect(value = HANADialect.class, comment = "HANA doesn't support comparing LOBs with the = operator")
@SkipForDialect(value = SybaseDialect.class, comment = "Sybase doesn't support comparing LOBs with the = operator")
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 jdbc driver doesn't support setNString")
@SkipForDialect(value = DerbyDialect.class, comment = "Derby jdbc driver doesn't support setNString")
public class StringMapLobTest extends BaseEnversJPAFunctionalTestCase {
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

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().remove( "3" );
			simple.getEmbeddedMap().put( "3", "Three-New" );
			entityManager.merge( simple );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().clear();
			entityManager.merge( simple );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), getAuditReader().getRevisions( Simple.class, 1 ) );
	}

	@Test
	public void testRevisionHistory() {
		final Simple rev1 = getAuditReader().find( Simple.class, 1, 1 );
		assertEquals( 2, rev1.getEmbeddedMap().entrySet().size() );
		TestTools.assertCollectionsEqual(
				TestTools.<String, String>mapBuilder()
						.add( "1", "One" )
						.add( "2", "Two" )
						.entries(),
				rev1.getEmbeddedMap().entrySet()
		);

		final Simple rev2 = getAuditReader().find( Simple.class, 1, 2 );
		assertEquals( 3, rev2.getEmbeddedMap().entrySet().size() );
		TestTools.assertCollectionsEqual(
				TestTools.<String,String>mapBuilder()
						.add( "1", "One" )
						.add( "2", "Two" )
						.add( "3", "Three" )
						.entries(),
				rev2.getEmbeddedMap().entrySet()
		);

		final Simple rev3 = getAuditReader().find( Simple.class, 1, 3 );
		assertEquals( 1, rev3.getEmbeddedMap().entrySet().size() );
		TestTools.assertCollectionsEqual(
				TestTools.<String,String>mapBuilder()
						.add( "3", "Three" )
						.entries(),
				rev3.getEmbeddedMap().entrySet()
		);

		final Simple rev4 = getAuditReader().find( Simple.class, 1, 4 );
		TestTools.assertCollectionsEqual(
				TestTools.<String,String>mapBuilder()
						.add( "3", "Three-New" )
						.entries(),
				rev4.getEmbeddedMap().entrySet()
		);

		final Simple rev5 = getAuditReader().find( Simple.class, 1, 5 );
		assertEquals( 0, rev5.getEmbeddedMap().size() );
	}

	@Entity(name = "Simple")
	@Audited
	public static class Simple {
		@Id
		private Integer id;
		private String name;

		@ElementCollection
		@Lob
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
