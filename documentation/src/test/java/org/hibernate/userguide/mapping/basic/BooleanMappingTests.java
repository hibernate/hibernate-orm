/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;

/**
 * Tests for mapping boolean values
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BooleanMappingTests.EntityOfBooleans.class )
@SessionFactory
public class BooleanMappingTests {
	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityOfBooleans.class );

		{
			final BasicAttributeMapping implicit = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "implicit" );
			final JdbcMapping jdbcMapping = implicit.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Boolean.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					// the implicit mapping will depend on the Dialect
					isOneOf( Types.BOOLEAN, Types.BIT, Types.TINYINT )
			);
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Converters

		{
			final BasicAttributeMapping convertedYesNo = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "convertedYesNo" );
			final JdbcMapping jdbcMapping = convertedYesNo.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Character.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					// could be NCHAR if nationalization is globally enabled
					isOneOf( Types.CHAR, Types.NCHAR )
			);
		}

		{
			final BasicAttributeMapping convertedTrueFalse = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "convertedTrueFalse" );
			final JdbcMapping jdbcMapping = convertedTrueFalse.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Character.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					// could be NCHAR if nationalization is globally enabled
					isOneOf( Types.CHAR, Types.NCHAR )
			);
		}

		{
			final BasicAttributeMapping convertedNumeric = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "convertedNumeric" );
			final JdbcMapping jdbcMapping = convertedNumeric.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Integer.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					equalTo( Types.INTEGER )
			);
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Legacy

		{
			final BasicAttributeMapping legacyYesNo = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "legacyYesNo" );
			final JdbcMapping jdbcMapping = legacyYesNo.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Character.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					// could be NCHAR if nationalization is globally enabled
					isOneOf( Types.CHAR, Types.NCHAR )
			);
		}

		{
			final BasicAttributeMapping legacyTrueFalse = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "legacyTrueFalse" );
			final JdbcMapping jdbcMapping = legacyTrueFalse.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Character.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					// could be NCHAR if nationalization is globally enabled
					isOneOf( Types.CHAR, Types.NCHAR )
			);
		}

		{
			final BasicAttributeMapping legacyNumeric = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "legacyNumeric" );
			final JdbcMapping jdbcMapping = legacyNumeric.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaType(), equalTo( Integer.class ) );
			assertThat(
					jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(),
					equalTo( Types.INTEGER )
			);
		}
	}

	@Entity( name = "EntityOfBooleans" )
	@Table( name = "EntityOfBooleans" )
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
		@Convert( converter = org.hibernate.type.YesNoConverter.class )
		boolean convertedYesNo;
		//end::basic-boolean-example-explicit-yes-no[]

		//tag::basic-boolean-example-explicit-t-f[]
		// this will get mapped to CHAR or NCHAR with a conversion
		@Basic
		@Convert( converter = org.hibernate.type.TrueFalseConverter.class )
		boolean convertedTrueFalse;
		//end::basic-boolean-example-explicit-t-f[]

		//tag::basic-boolean-example-explicit-numeric[]
		// this will get mapped to TINYINT with a conversion
		@Basic
		@Convert( converter = org.hibernate.type.NumericBooleanConverter.class )
		boolean convertedNumeric;
		//end::basic-boolean-example-explicit-numeric[]


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Legacy

		//tag::basic-boolean-example-explicit-legacy-yes-no[]
		// this will get mapped to CHAR or NCHAR with a conversion
		@Basic
		@org.hibernate.annotations.Type( type = "yes_no" )
		boolean legacyYesNo;
		//end::basic-boolean-example-explicit-legacy-yes-no[]

		//tag::basic-boolean-example-explicit-legacy-t-f[]
		// this will get mapped to CHAR or NCHAR with a conversion
		@Basic
		@org.hibernate.annotations.Type( type = "true_false" )
		boolean legacyTrueFalse;
		//end::basic-boolean-example-explicit-legacy-t-f[]

		//tag::basic-boolean-example-explicit-legacy-numeric[]
		// this will get mapped to TINYINT with a conversion
		@Basic
		@org.hibernate.annotations.Type( type = "numeric_boolean" )
		boolean legacyNumeric;
		//end::basic-boolean-example-explicit-legacy-numeric[]
	}
}
