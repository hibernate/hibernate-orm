/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Nationalized;
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

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = CharacterArrayMappingTests.EntityWithCharArrays.class )
@SessionFactory
public class CharacterArrayMappingTests {
	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityWithCharArrays.class );

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "primitive" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.VARCHAR ) );
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "wrapper" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.VARCHAR ) );
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "primitiveNVarchar" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.NVARCHAR ) );
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "wrapperNVarchar" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.NVARCHAR ) );
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "primitiveClob" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.CLOB ) );
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "wrapperClob" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.CLOB ) );
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "primitiveNClob" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.NCLOB ) );
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "wrapperNClob" );
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.NCLOB ) );
		}
	}

	@Entity( name = "EntityWithCharArrays" )
	@Table( name = "EntityWithCharArrays" )
	public static class EntityWithCharArrays {
		@Id
		public Integer id;

		//tag::basic-chararray-examples[]
		// mapped as VARCHAR
		char[] primitive;
		Character[] wrapper;

		// mapped as NVARCHAR
		@Nationalized
		char[] primitiveNVarchar;
		@Nationalized
		Character[] wrapperNVarchar;

		// mapped as CLOB
		@Lob
		char[] primitiveClob;
		@Lob
		Character[] wrapperClob;

		// mapped as NCLOB
		@Lob
		@Nationalized
		char[] primitiveNClob;
		@Lob
		@Nationalized
		Character[] wrapperNClob;
		//end::basic-chararray-examples[]
	}
}
