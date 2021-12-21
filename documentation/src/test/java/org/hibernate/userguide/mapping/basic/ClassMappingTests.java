/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.time.ZoneOffset;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = ClassMappingTests.EntityWithClass.class )
@SessionFactory
public class ClassMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final JdbcTypeRegistry jdbcRegistry = domainModel.getTypeConfiguration().getJdbcTypeDescriptorRegistry();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityWithClass.class );

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "clazz" );
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( Class.class ) );
		assertThat( jdbcMapping.getJdbcTypeDescriptor(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR ) ) );

		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithClass( 1, String.class ) );
				}
		);

		scope.inTransaction(
				(session) -> session.find( EntityWithClass.class, 1 )
		);
	}

	@Entity( name = "EntityWithClass" )
	@Table( name = "EntityWithClass" )
	public static class EntityWithClass {
		@Id
		private Integer id;

		//tag::basic-Class-example[]
		// mapped as VARCHAR
		private Class<?> clazz;
		//end::basic-Class-example[]

		public EntityWithClass() {
		}

		public EntityWithClass(Integer id, Class<?> clazz) {
			this.id = id;
			this.clazz = clazz;
		}
	}
}
