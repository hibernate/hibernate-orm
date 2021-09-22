/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for mapping `double` values
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = StringMappingTests.EntityOfStrings.class )
@SessionFactory
public class StringMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityOfStrings.class );

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "string" );
			assertThat( attribute.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( String.class ) );

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( String.class ) );
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.VARCHAR ) );
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "clobString" );
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( String.class ) );
			assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.CLOB ) );
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist( new EntityOfStrings( 1, "string", "clob" ) )
		);
		scope.inTransaction(
				(session) -> session.get( EntityOfStrings.class, 1 )
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete EntityOfStrings" ).executeUpdate()
		);
	}

	@Entity( name = "EntityOfStrings" )
	@Table( name = "EntityOfStrings" )
	public static class EntityOfStrings {
		@Id
		Integer id;

		//tag::basic-string-example[]
		// will be mapped using VARCHAR
		String string;

		// will be mapped using CLOB
		@Lob
		String clobString;
		//end::basic-string-example[]

		public EntityOfStrings() {
		}

		public EntityOfStrings(Integer id, String string, String clobString) {
			this.id = id;
			this.string = string;
			this.clobString = clobString;
		}
	}
}
