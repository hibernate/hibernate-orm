/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;

/**
 * Tests for mapping boolean values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = BooleanMappingTests.EntityOfBooleans.class)
@SessionFactory
public class BooleanMappingTests {
	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor(EntityOfBooleans.class);

		{
			final BasicAttributeMapping implicit = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("implicit");
			final JdbcMapping jdbcMapping = implicit.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo(Boolean.class));
			assertThat(
					jdbcMapping.getJdbcType().getJdbcTypeCode(),
					// the implicit mapping will depend on the Dialect
					isOneOf( Types.BOOLEAN, Types.BIT, Types.TINYINT, Types.SMALLINT )
			);
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Converters

		{
			final BasicAttributeMapping convertedYesNo = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("convertedYesNo");
			final JdbcMapping jdbcMapping = convertedYesNo.getJdbcMapping();
			assertThat( jdbcMapping, instanceOf( ConvertedBasicTypeImpl.class ) );
			assertThat( jdbcMapping.getJdbcJavaType().getJavaType(), equalTo( Character.class ) );
			assertThat(
					jdbcMapping.getJdbcType().getJdbcTypeCode(),
					// could be NCHAR if nationalization is globally enabled
					isOneOf( Types.CHAR, Types.NCHAR )
			);
		}

		{
			final BasicAttributeMapping convertedTrueFalse = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("convertedTrueFalse");
			final JdbcMapping jdbcMapping = convertedTrueFalse.getJdbcMapping();
			assertThat( jdbcMapping, instanceOf( ConvertedBasicTypeImpl.class ) );
			assertThat( jdbcMapping.getJdbcJavaType().getJavaType(), equalTo( Character.class ) );
			assertThat(
					jdbcMapping.getJdbcType().getJdbcTypeCode(),
					// could be NCHAR if nationalization is globally enabled
					isOneOf( Types.CHAR, Types.NCHAR )
			);
		}

		{
			final BasicAttributeMapping convertedNumeric = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("convertedNumeric");
			final JdbcMapping jdbcMapping = convertedNumeric.getJdbcMapping();
			assertThat( jdbcMapping, instanceOf( ConvertedBasicTypeImpl.class ) );
			assertThat( jdbcMapping.getJdbcJavaType().getJavaType(), equalTo( Integer.class ) );
			assertThat(
					jdbcMapping.getJdbcType().getJdbcTypeCode(),
					equalTo( Types.INTEGER )
			);
		}
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOfBooleans entity = new EntityOfBooleans();
			entity.id = 1;
			assert !entity.convertedYesNo;
			assert !entity.convertedTrueFalse;
			assert !entity.convertedNumeric;
			session.persist( entity );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete EntityOfBooleans" ).executeUpdate();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16182" )
	public void testComparisonLiteralHandling(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedYesNo = true" ).list(),
					hasSize( 0 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedTrueFalse = true" ).list(),
					hasSize( 0 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedNumeric = true" ).list(),
					hasSize( 0 )
			);
		} );

		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedYesNo = false" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedTrueFalse = false" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedNumeric = false" ).list(),
					hasSize( 1 )
			);
		} );

		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedYesNo != true" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedTrueFalse != true" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedNumeric != true" ).list(),
					hasSize( 1 )
			);
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16182" )
	public void testExpressionAsPredicateUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedYesNo" ).list(),
					hasSize( 0 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedTrueFalse" ).list(),
					hasSize( 0 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedNumeric" ).list(),
					hasSize( 0 )
			);
		} );

		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where (convertedYesNo)" ).list(),
					hasSize( 0 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where (convertedTrueFalse)" ).list(),
					hasSize( 0 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where (convertedNumeric)" ).list(),
					hasSize( 0 )
			);
		} );
	}


	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16182" )
	public void testNegatedExpressionAsPredicateUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where not convertedYesNo" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where not convertedTrueFalse" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where not convertedNumeric" ).list(),
					hasSize( 1 )
			);
		} );
		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where not (convertedYesNo)" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where not (convertedTrueFalse)" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where not (convertedNumeric)" ).list(),
					hasSize( 1 )
			);
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16182" )
	public void testSetClauseUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat(
					session.createMutationQuery( "update EntityOfBooleans set convertedYesNo = true" ).executeUpdate(),
					equalTo( 1 )
			);
			assertThat(
					session.createMutationQuery( "update EntityOfBooleans set convertedTrueFalse = true" ).executeUpdate(),
					equalTo( 1 )
			);
			assertThat(
					session.createMutationQuery( "update EntityOfBooleans set convertedNumeric = true" ).executeUpdate(),
					equalTo( 1 )
			);
		} );

		scope.inTransaction( (session) -> {
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedYesNo" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedTrueFalse" ).list(),
					hasSize( 1 )
			);
			assertThat(
					session.createSelectionQuery( "from EntityOfBooleans where convertedNumeric" ).list(),
					hasSize( 1 )
			);
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16182" )
	public void testCriteriaUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat( countByCriteria( "convertedYesNo", true, session ), equalTo( 0 ) );
			assertThat( countByCriteria( "convertedTrueFalse", true, session ), equalTo( 0 ) );
			assertThat( countByCriteria( "convertedNumeric", true, session ), equalTo( 0 ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16182" )
	public void testNegatedCriteriaUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat( countByCriteria( "convertedYesNo", false, session ), equalTo( 1 ) );
			assertThat( countByCriteria( "convertedTrueFalse", false, session ), equalTo( 1 ) );
			assertThat( countByCriteria( "convertedNumeric", false, session ), equalTo( 1 ) );
		} );
	}

	private int countByCriteria(String attributeName, boolean matchValue, SessionImplementor session) {
		final HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Long> criteria = builder.createQuery( Long.class );
		criteria.select( builder.count( builder.literal( 1 ) ) );
		final JpaRoot<EntityOfBooleans> root = criteria.from( EntityOfBooleans.class );
		final JpaPath<Boolean> convertedYesNo = root.get( attributeName );
		if ( matchValue ) {
			criteria.where( convertedYesNo );
		}
		else {
			criteria.where( builder.not( convertedYesNo ) );
		}

		final Long result = session.createQuery( criteria ).uniqueResult();
		return result.intValue();
	}

	@Entity(name = "EntityOfBooleans")
	@Table(name = "EntityOfBooleans")
	public static class EntityOfBooleans {
		@Id
		Integer id;

		//tag::basic-boolean-example-implicit[]
		// this will be mapped to BIT or BOOLEAN on the database
		@Basic
		boolean implicit;
		//end::basic-boolean-example-implicit[]


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// converted

		//tag::basic-boolean-example-explicit-yes-no[]
		// this will get mapped to CHAR or NCHAR with a conversion
		@Basic
		@Convert(converter = org.hibernate.type.YesNoConverter.class)
		boolean convertedYesNo;
		//end::basic-boolean-example-explicit-yes-no[]

		//tag::basic-boolean-example-explicit-t-f[]
		// this will get mapped to CHAR or NCHAR with a conversion
		@Basic
		@Convert(converter = org.hibernate.type.TrueFalseConverter.class)
		boolean convertedTrueFalse;
		//end::basic-boolean-example-explicit-t-f[]

		//tag::basic-boolean-example-explicit-numeric[]
		// this will get mapped to TINYINT with a conversion
		@Basic
		@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
		boolean convertedNumeric;
		//end::basic-boolean-example-explicit-numeric[]
	}
}
