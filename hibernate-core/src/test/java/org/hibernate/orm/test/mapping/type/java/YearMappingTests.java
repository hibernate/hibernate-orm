/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;

import jakarta.persistence.*;
import org.assertj.core.api.Assertions;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.YearJavaType;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = YearMappingTests.YearMappingTestEntity.class )
@SessionFactory
@JiraKey( "HHH-10558" )
@JiraKey( "HHH-17507" )
public class YearMappingTests {
	@Test
	public void basicAssertions(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor( YearMappingTestEntity.class );

		{
			final BasicAttributeMapping yearAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "year" );
			assertThat( yearAttribute.getJdbcMapping().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
			assertThat( yearAttribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Year.class );
		}

		{
			final PluralAttributeMapping yearsAttribute = (PluralAttributeMapping) entityDescriptor.findAttributeMapping( "years" );
			final BasicValuedCollectionPart elementDescriptor = (BasicValuedCollectionPart) yearsAttribute.getElementDescriptor();
			assertThat( elementDescriptor.getJdbcMapping().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
			assertThat( elementDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Year.class );
		}

		{
			final PluralAttributeMapping countByYearAttribute = (PluralAttributeMapping) entityDescriptor.findAttributeMapping( "countByYear" );
			final BasicValuedCollectionPart keyDescriptor = (BasicValuedCollectionPart) countByYearAttribute.getIndexDescriptor();
			assertThat( keyDescriptor.getJdbcMapping().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
			assertThat( keyDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Year.class );
		}
	}

	@Test
	public void testUsage(SessionFactoryScope scope) {
		final YearMappingTestEntity entity1 = new YearMappingTestEntity( 1, "one", Year.now() );
		final YearMappingTestEntity entity2 = new YearMappingTestEntity( 2, "two", Year.parse( "+10000" ) );

		scope.inTransaction( (session) -> {
			session.persist( entity1 );
			session.persist( entity2 );
		} );

		try {
			scope.inTransaction( (session) -> session.createQuery( "from YearMappingTestEntity", YearMappingTestEntity.class ).list() );
		}
		finally {
			scope.inTransaction( session -> session.remove( entity1 ) );
			scope.inTransaction( session -> session.remove( entity2 ) );
		}
	}
	@Test
	public void testUnwrapPass() {
		final YearJavaType yearJavaType = new YearJavaType();
		final Year year = Year.of(1943);
		{
			final Year y = yearJavaType.unwrap(year, Year.class, null);
			Assertions.assertThat( y ).isEqualTo( year );
		}
		{
			final Integer y = yearJavaType.unwrap(year, Integer.class, null);
			Assertions.assertThat( y ).isEqualTo( Integer.valueOf( 1943 ) );
		}
		{
			final Long y = yearJavaType.unwrap(year, Long.class, null);
			Assertions.assertThat( y ).isEqualTo( Long.valueOf( 1943L ) );
		}
		{
			final String y = yearJavaType.unwrap(year, String.class, null);
			Assertions.assertThat( y ).isEqualTo( "1943" );
		}
		{
			final Object y = yearJavaType.unwrap(year, Object.class, null);
			Assertions.assertThat( y.toString() ).isEqualTo( "1943" );
		}
	}

	@Test
	public void testUnwrapFail() {
		final YearJavaType yearJavaType = new YearJavaType();
		final Year year = Year.of(1943);
		{
			Assertions.assertThatThrownBy( () ->
				yearJavaType.unwrap(year, Boolean.class, null)
			).isInstanceOf( HibernateException.class );
		}
	}

	@Test
	public void testWrapPass() {
		final YearJavaType yearJavaType = new YearJavaType();
		{
			final Year usingNull = yearJavaType.wrap( null, null );
			Assertions.assertThat( usingNull ).isNull();
		}
		{
			final Year usingNumber = yearJavaType.wrap( 1943, null );
			Assertions.assertThat( usingNumber ).isNotNull();
		}
		{
			final Year usingNegative = yearJavaType.wrap( -1, null );
			Assertions.assertThat( usingNegative ).isNotNull();
		}
		{
			final Year usingString = yearJavaType.wrap( "1943", null );
			Assertions.assertThat( usingString ).isNotNull();
		}
		{
			final Year usingYear = yearJavaType.wrap( Year.of( 1943), null );
			Assertions.assertThat( usingYear ).isNotNull();
		}
	}

	@Test
	public void testWrapFail() {
		final YearJavaType yearJavaType = new YearJavaType();
		{
			final String usingEmptyString = "";
			Assertions.assertThatThrownBy(() ->
				yearJavaType.wrap( usingEmptyString, null )
			).isInstanceOf(DateTimeParseException.class);
		}
		{
			final Date usingDate = new Date();
			Assertions.assertThatThrownBy(() ->
				yearJavaType.wrap( usingDate, null )
			).isInstanceOf(HibernateException.class);
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
