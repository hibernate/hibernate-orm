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

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-9834")
@EnversTest
@Jpa(annotatedClasses = {StringMapNationalizedLobTest.Simple.class})
@SkipForDialect(dialectClass = OracleDialect.class)
@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "@Lob field in HQL predicate fails with error about text = bigint")
@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA doesn't support comparing LOBs with the = operator")
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase doesn't support comparing LOBs with the = operator")
@SkipForDialect(dialectClass = DB2Dialect.class, matchSubTypes = true, reason = "DB2 jdbc driver doesn't support setNString")
@SkipForDialect(dialectClass = DerbyDialect.class, matchSubTypes = true, reason = "Derby jdbc driver doesn't support setNString")
public class StringMapNationalizedLobTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Simple simple = new Simple( 1, "Simple" );
			simple.getEmbeddedMap().put( "1", "One" );
			simple.getEmbeddedMap().put( "2", "Two" );
			entityManager.persist( simple );
		} );

		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().put( "3", "Three" );
			entityManager.merge( simple );
		} );

		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().remove( "1" );
			simple.getEmbeddedMap().remove( "2" );
			entityManager.merge( simple );
		} );

		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().remove( "3" );
			simple.getEmbeddedMap().put( "3", "Three-New" );
			entityManager.merge( simple );
		} );

		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, 1 );
			simple.getEmbeddedMap().clear();
			entityManager.merge( simple );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( Simple.class, 1 ) );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			final Simple rev1 = auditReader.find( Simple.class, 1, 1 );
			assertEquals( 2, rev1.getEmbeddedMap().entrySet().size() );
			TestTools.assertCollectionsEqual(
					TestTools.<String, String>mapBuilder()
							.add( "1", "One" )
							.add( "2", "Two" )
							.entries(),
					rev1.getEmbeddedMap().entrySet()
			);

			final Simple rev2 = auditReader.find( Simple.class, 1, 2 );
			assertEquals( 3, rev2.getEmbeddedMap().entrySet().size() );
			TestTools.assertCollectionsEqual(
					TestTools.<String,String>mapBuilder()
							.add( "1", "One" )
							.add( "2", "Two" )
							.add( "3", "Three" )
							.entries(),
					rev2.getEmbeddedMap().entrySet()
			);

			final Simple rev3 = auditReader.find( Simple.class, 1, 3 );
			assertEquals( 1, rev3.getEmbeddedMap().entrySet().size() );
			TestTools.assertCollectionsEqual(
					TestTools.<String,String>mapBuilder()
							.add( "3", "Three" )
							.entries(),
					rev3.getEmbeddedMap().entrySet()
			);

			final Simple rev4 = auditReader.find( Simple.class, 1, 4 );
			TestTools.assertCollectionsEqual(
					TestTools.<String,String>mapBuilder()
							.add( "3", "Three-New" )
							.entries(),
					rev4.getEmbeddedMap().entrySet()
			);

			final Simple rev5 = auditReader.find( Simple.class, 1, 5 );
			assertEquals( 0, rev5.getEmbeddedMap().size() );
		} );
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
