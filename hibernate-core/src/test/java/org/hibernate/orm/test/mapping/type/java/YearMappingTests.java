/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;

import java.sql.Types;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = YearMappingTests.YearMappingTestEntity.class )
@SessionFactory
@JiraKey( "HHH-10558" )
public class YearMappingTests {
	@Test
	public void basicAssertions(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMetamodel().entityPersister( YearMappingTestEntity.class );

		{
			final BasicAttributeMapping yearAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "year" );
			assertThat( yearAttribute.getJdbcMapping().getJdbcTypeDescriptor().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
			assertThat( yearAttribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Year.class );
		}

		{
			final PluralAttributeMapping yearsAttribute = (PluralAttributeMapping) entityDescriptor.findAttributeMapping( "years" );
			final BasicValuedCollectionPart elementDescriptor = (BasicValuedCollectionPart) yearsAttribute.getElementDescriptor();
			assertThat( elementDescriptor.getJdbcMapping().getJdbcTypeDescriptor().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
			assertThat( elementDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Year.class );
		}

		{
			final PluralAttributeMapping countByYearAttribute = (PluralAttributeMapping) entityDescriptor.findAttributeMapping( "countByYear" );
			final BasicValuedCollectionPart keyDescriptor = (BasicValuedCollectionPart) countByYearAttribute.getIndexDescriptor();
			assertThat( keyDescriptor.getJdbcMapping().getJdbcTypeDescriptor().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
			assertThat( keyDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Year.class );
		}
	}

	@Test
	public void testUsage(SessionFactoryScope scope) {
		final YearMappingTestEntity entity = new YearMappingTestEntity( 1, "one", Year.now() );
		final YearMappingTestEntity entity2 = new YearMappingTestEntity( 2, "two", Year.parse( "+10000" ) );

		scope.inTransaction( (session) -> {
			session.save( entity );
			session.save( entity2 );
		} );

		try {
			scope.inTransaction( (session) -> session.createQuery( "from YearMappingTestEntity" ).list() );
		}
		finally {
			scope.inTransaction( session -> session.delete( entity ) );
			scope.inTransaction( session -> session.delete( entity2 ) );
		}
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

		@Column( name = "yr" )
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
		@Column( name = "years" )
		public Set<Year> getYears() {
			return years;
		}

		public void setYears(Set<Year> years) {
			this.years = years;
		}

		@ElementCollection
		@CollectionTable( name = "count_by_year", joinColumns = @JoinColumn( name = "entity_id" ) )
		@MapKeyColumn( name = "yr" )
		@Column( name = "cnt" )
		public Map<Year, Integer> getCountByYear() {
			return countByYear;
		}

		public void setCountByYear(Map<Year, Integer> countByYear) {
			this.countByYear = countByYear;
		}
	}
}
