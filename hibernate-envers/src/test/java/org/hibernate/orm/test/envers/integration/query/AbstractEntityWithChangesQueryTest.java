/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-8058")
public abstract class AbstractEntityWithChangesQueryTest {
	private Integer simpleId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		simpleId = scope.fromTransaction( entityManager -> {
			final Simple simple = new Simple();
			simple.setName( "Name" );
			simple.setValue( 25 );
			entityManager.persist( simple );
			return simple.getId();
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, simpleId );
			simple.setName( "Name-Modified2" );
			entityManager.merge( simple );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, simpleId );
			simple.setName( "Name-Modified3" );
			simple.setValue( 100 );
			entityManager.merge( simple );
		} );

		// Revision 4
		scope.inTransaction( entityManager -> {
			final Simple simple = entityManager.find( Simple.class, simpleId );
			entityManager.remove( simple );
		} );
	}

	@Test
	public void testRevisionCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), AuditReaderFactory.get( em ).getRevisions( Simple.class, simpleId ) );
		} );
	}

	@Test
	public void testEntityRevisionsWithChangesQueryNoDeletions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntityWithChanges( Simple.class, false )
					.add( AuditEntity.id().eq( simpleId ) )
					.getResultList();
			compareResults( getExpectedResults( scope, false ), results );
		} );
	}

	@Test
	public void testEntityRevisionsWithChangesQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntityWithChanges( Simple.class, true )
					.add( AuditEntity.id().eq( simpleId ) )
					.getResultList();
			compareResults( getExpectedResults( scope, true ), results );
		} );
	}

	private void compareResults(List<Object[]> expectedResults, List results) {
		assertEquals( expectedResults.size(), results.size() );
		for ( int i = 0; i < results.size(); ++i ) {
			final Object[] row = (Object[]) results.get( i );
			final Object[] expectedRow = expectedResults.get( i );
			// the query returns 4, index 1 has the revision entity which we don't test here
			assertEquals( 4, row.length );
			// because we don't test the revision entity, we adjust indexes between the two arrays
			assertEquals( expectedRow[ 0 ], row[ 0 ] );
			assertEquals( expectedRow[ 1 ], row[ 2 ] );
			assertEquals( expectedRow[ 2 ], row[ 3 ] );
		}
	}

	protected List<Object[]> getExpectedResults(EntityManagerFactoryScope scope, boolean includeDeletions) {

		String deleteName = null;
		Integer deleteValue = null;
		if ( Boolean.TRUE.toString().equals( scope.getEntityManagerFactory().getProperties().get( EnversSettings.STORE_DATA_AT_DELETE ) ) ) {
			deleteName = "Name-Modified3";
			deleteValue = 100;
		}

		final List<Object[]> results = new ArrayList<>();

		results.add(
				new Object[] {
						new Simple( simpleId, "Name", 25 ),
						RevisionType.ADD,
						Collections.emptySet()
				}
		);

		results.add(
				new Object[] {
						new Simple( simpleId, "Name-Modified2", 25 ),
						RevisionType.MOD,
						TestTools.makeSet( "name" )
				}
		);

		results.add(
				new Object[] {
						new Simple( simpleId, "Name-Modified3", 100 ),
						RevisionType.MOD,
						TestTools.makeSet( "name", "value" )
				}
		);

		if ( includeDeletions ) {
			results.add(
					new Object[] {
							new Simple( simpleId, deleteName, deleteValue ),
							RevisionType.DEL,
							Collections.emptySet()
					}
			);
		}

		System.out.println( "Generated " + results.size() + " results." );
		return results;
	}

	@Audited
	@Entity(name = "Simple")
	public static class Simple {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@Column(name = "val")
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
