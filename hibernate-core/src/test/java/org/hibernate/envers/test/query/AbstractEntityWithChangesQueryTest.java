/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-8058" )
public abstract class AbstractEntityWithChangesQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer simpleId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Revision 1
		simpleId = inTransaction(
				entityManager -> {
					final Simple simple = new Simple();
					simple.setName( "Name" );
					simple.setValue( 25 );
					entityManager.persist( simple );
					return simple.getId();
				} );

		// Revision 2
		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, simpleId );
					simple.setName( "Name-Modified2" );
					entityManager.merge( simple );
				}
		);

		// Revision 3
		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, simpleId );
					simple.setName( "Name-Modified3" );
					simple.setValue( 100 );
					entityManager.merge( simple );
				}
		);

		// Revision 4
		inTransaction(
				entityManager -> {
					final Simple simple = entityManager.find( Simple.class, simpleId );
					entityManager.remove( simple );
				}
		);
	}

	@DynamicTest
	public void testRevisionCount() {
		assertThat( getAuditReader().getRevisions( Simple.class, simpleId ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testEntityRevisionsWithChangesQueryNoDeletions() {
		List results = getAuditReader().createQuery()
				.forRevisionsOfEntityWithChanges( Simple.class, false )
				.add( AuditEntity.id().eq( simpleId ) )
				.getResultList();
		assertResults( getExpectedResults( false ), results );
	}

	@DynamicTest
	public void testEntityRevisionsWithChangesQuery() {
		List results = getAuditReader().createQuery()
				.forRevisionsOfEntityWithChanges( Simple.class, true )
				.add( AuditEntity.id().eq( simpleId ) )
				.getResultList();
		assertResults( getExpectedResults( true ), results );
	}

	private void assertResults(List<Object[]> expected, List results) {
		assertThat( results, CollectionMatchers.hasSize( expected.size() ) );
		for ( int i = 0; i < results.size(); ++i ) {
			final Object[] row = (Object[]) results.get( i );
			final Object[] expectedRow = expected.get( i );
			// the query returns 4, index 1 has the revision entity which we don't test here
			assertThat( row.length, equalTo( 4 ) );
			for ( int j = 0; j < 4; ++j ) {
				// Skip index 1 because thats the revision entity and we aren't concerned with it.
				if ( j != 1 ) {
					assertThat( row[ j ], equalTo( expectedRow[ j ] ) );
				}
			}
		}
	}

	protected List<Object[]> getExpectedResults(boolean includeDeletions) {

		String deleteName = null;
		Integer deleteValue = null;
		final Map<String, Object> properties = entityManagerFactoryScope().getEntityManagerFactory().getProperties();
		if ( "true".equals( properties.get( EnversSettings.STORE_DATA_AT_DELETE ) ) ) {
			deleteName = "Name-Modified3";
			deleteValue = 100;
		}

		final List<Object[]> results = new ArrayList<>();

		results.add(
				new Object[] {
						new Simple( simpleId, "Name", 25 ),
						null,
						RevisionType.ADD,
						Collections.emptySet()
				}
		);

		results.add(
				new Object[] {
						new Simple( simpleId, "Name-Modified2", 25 ),
						null,
						RevisionType.MOD,
						new HashSet<>( Arrays.asList( "name" ) )
				}
		);

		results.add(
				new Object[] {
						new Simple( simpleId, "Name-Modified3", 100 ),
						null,
						RevisionType.MOD,
						new HashSet<>( Arrays.asList( "name", "value" ) )
				}
		);

		if ( includeDeletions ) {
			results.add(
					new Object[] {
							new Simple( simpleId, deleteName, deleteValue ),
							null,
							RevisionType.DEL,
							Collections.emptySet()
					}
			);
		}

		return results;
	}

	@Audited
	@Entity(name = "Simple")
	public static class Simple {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		private Integer value;

		Simple() {

		}

		Simple(Integer id, String name, Integer value) {
			this.id = id;
			this.name = name;
			this.value = value;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Simple simple = (Simple) o;

			if ( getId() != null ? !getId().equals( simple.getId() ) : simple.getId() != null ) {
				return false;
			}
			if ( getName() != null ? !getName().equals( simple.getName() ) : simple.getName() != null ) {
				return false;
			}
			return getValue() != null ? getValue().equals( simple.getValue() ) : simple.getValue() == null;
		}

		@Override
		public int hashCode() {
			int result = getId() != null ? getId().hashCode() : 0;
			result = 31 * result + ( getName() != null ? getName().hashCode() : 0 );
			result = 31 * result + ( getValue() != null ? getValue().hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			return "Simple{" +
					"id=" + id +
					", name='" + name + '\'' +
					", value=" + value +
					'}';
		}
	}
}
