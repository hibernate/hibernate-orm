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

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for mapping strings as nationalized data (NCLOB, NVARCHAR)
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = NationalizedStringMappingTests.EntityOfNationalizedStrings.class )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsNationalizedData.class )
public class NationalizedStringMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityOfNationalizedStrings.class );

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "nstring" );
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( String.class ) );
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.NVARCHAR ) );
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "nclobString" );
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( String.class ) );
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.NCLOB ) );
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist( new EntityOfNationalizedStrings( 1, "nstring", "nclob" ) )
		);
		scope.inTransaction(
				(session) -> session.get( EntityOfNationalizedStrings.class, 1 )
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete EntityOfNationalizedStrings" ).executeUpdate()
		);
	}

	@Entity( name = "EntityOfNationalizedStrings" )
	@Table( name = "EntityOfNationalizedStrings" )
	public static class EntityOfNationalizedStrings {
		@Id
		Integer id;

		//tag::basic-nstring-example-implicit[]
		// will be mapped using NVARCHAR
		@Nationalized
		String nstring;

		// will be mapped using NCLOB
		@Lob
		@Nationalized
		String nclobString;
		//end::basic-nstring-example-implicit[]

		public EntityOfNationalizedStrings() {
		}

		public EntityOfNationalizedStrings(Integer id, String nstring, String nclobString) {
			this.id = id;
			this.nstring = nstring;
			this.nclobString = nclobString;
		}
	}
}
