/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.Types;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test support for mapping {@link java.time.Year} values
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-10558" )
public class YearMappingTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void basicAssertions() {
		{
			final EntityPersister entityDescriptor = sessionFactory().getMetamodel().entityPersister( YearMappingTestEntity.class );
			final Type yearPropertyDescriptor = entityDescriptor.getPropertyType( "year" );
			final int[] sqlTypes = yearPropertyDescriptor.sqlTypes( sessionFactory() );
			assertThat( sqlTypes.length, is( 1 ) );
			assertThat( sqlTypes[0], is( Types.INTEGER ) );
		}

		{
			final CollectionPersister yearsAttributeDescriptor = sessionFactory().getMetamodel()
					.collectionPersister( YearMappingTestEntity.class.getName() + ".years" );
			final int[] sqlTypes = yearsAttributeDescriptor.getElementType().sqlTypes( sessionFactory() );
			assertThat( sqlTypes.length, is( 1 ) );
			assertThat( sqlTypes[0], is( Types.INTEGER ) );
		}

		{
			final CollectionPersister countByYearAttributeDescriptor = sessionFactory().getMetamodel()
					.collectionPersister( YearMappingTestEntity.class.getName() + ".countByYear" );
			final int[] sqlTypes = countByYearAttributeDescriptor.getIndexType().sqlTypes( sessionFactory() );
			assertThat( sqlTypes.length, is( 1 ) );
			assertThat( sqlTypes[0], is( Types.INTEGER ) );
		}
	}

	@Test
	public void testUsage() {
		final YearMappingTestEntity entity = new YearMappingTestEntity( 1, "one", Year.now() );
		inTransaction( session -> session.save( entity ) );

		try {
			inTransaction( session -> session.createQuery( "from YearMappingTestEntity" ).list() );
		}
		finally {
			inTransaction( session -> session.delete( entity ) );
		}
	}


	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( YearMappingTestEntity.class );
	}

	@Entity( name = "YearMappingTestEntity" )
	@Table( name = "year_map_test_entity" )
	public static class YearMappingTestEntity {
		private Integer id;
		private String name;
		private Year year;
		private Set<Year> years = new HashSet<>();
		private Map<Year,Integer> countByYear = new HashMap<>();

		public YearMappingTestEntity() {
		}

		public YearMappingTestEntity(Integer id, String name, Year year) {
			this.id = id;
			this.name = name;
			this.year = year;
		}

		@Id
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

		public Year getYear() {
			return year;
		}

		public void setYear(Year year) {
			this.year = year;
		}

		@ElementCollection
		@CollectionTable(
				name = "entity_year",
				joinColumns = @JoinColumn( name = "entity_id" )
		)
		@Column( name = "year" )
		public Set<Year> getYears() {
			return years;
		}

		public void setYears(Set<Year> years) {
			this.years = years;
		}

		@ElementCollection
		@CollectionTable( name = "count_by_year", joinColumns = @JoinColumn( name = "entity_id" ) )
		@MapKeyColumn( name = "year" )
		@Column( name = "cnt" )
		public Map<Year, Integer> getCountByYear() {
			return countByYear;
		}

		public void setCountByYear(Map<Year, Integer> countByYear) {
			this.countByYear = countByYear;
		}
	}
}
