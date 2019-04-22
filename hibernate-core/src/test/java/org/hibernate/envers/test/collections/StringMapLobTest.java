/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-9834")
@SkipForDialect(Oracle8iDialect.class)
@SkipForDialect(value = PostgreSQL81Dialect.class, jiraKey = "HHH-11477", comment = "@Lob field in HQL predicate fails with error about text = bigint")
@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA doesn't support comparing LOBs with the = operator")
@Disabled("NYI - embeddedMap is being created as a varchar(255) rather than clob field")
public class StringMapLobTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final Simple simple = new Simple( 1, "Simple" );
					simple.getEmbeddedMap().put( "1", "One" );
					simple.getEmbeddedMap().put( "2", "Two" );
					entityManager.persist( simple );
				}
		);

		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, 1 );
					simple.getEmbeddedMap().put( "3", "Three" );
					entityManager.merge( simple );
				}
		);

		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, 1 );
					simple.getEmbeddedMap().remove( "1" );
					simple.getEmbeddedMap().remove( "2" );
					entityManager.merge( simple );
				}
		);

		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, 1 );
					simple.getEmbeddedMap().remove( "3" );
					simple.getEmbeddedMap().put( "3", "Three-New" );
					entityManager.merge( simple );
				}
		);

		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, 1 );
					simple.getEmbeddedMap().clear();
					entityManager.merge( simple );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Simple.class, 1 ), contains( 1, 2, 3, 4, 5 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		final Simple rev1 = getAuditReader().find( Simple.class, 1, 1 );
		assertThat( rev1.getEmbeddedMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
		assertThat( rev1.getEmbeddedMap(), hasEntry( "1", "One" ) );
		assertThat( rev1.getEmbeddedMap(), hasEntry( "2", "Two" ) );

		final Simple rev2 = getAuditReader().find( Simple.class, 1, 2 );
		assertThat( rev2.getEmbeddedMap().entrySet(), CollectionMatchers.hasSize( 3 ) );
		assertThat( rev2.getEmbeddedMap(), hasEntry( "1", "One" ) );
		assertThat( rev2.getEmbeddedMap(), hasEntry( "2", "Two" ) );
		assertThat( rev2.getEmbeddedMap(), hasEntry( "3", "Three" ) );

		final Simple rev3 = getAuditReader().find( Simple.class, 1, 3 );
		assertThat( rev3.getEmbeddedMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
		assertThat( rev3.getEmbeddedMap(), hasEntry( "3", "Three" ) );

		final Simple rev4 = getAuditReader().find( Simple.class, 1, 4 );
		assertThat( rev4.getEmbeddedMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
		assertThat( rev4.getEmbeddedMap(), hasEntry( "3", "Three-New" ) );

		final Simple rev5 = getAuditReader().find( Simple.class, 1, 5 );
		assertThat( rev5.getEmbeddedMap().entrySet(), CollectionMatchers.isEmpty() );
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
