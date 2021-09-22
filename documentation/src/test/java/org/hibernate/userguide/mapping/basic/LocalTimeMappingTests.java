/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.time.LocalTime;
import jakarta.persistence.Column;
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

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = LocalTimeMappingTests.EntityWithLocalTime.class )
@SessionFactory
public class LocalTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityWithLocalTime.class );

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "localTime" );
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( LocalTime.class ) );
		assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.TIME ) );

		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithLocalTime( 1, LocalTime.now() ) );
				}
		);

		scope.inTransaction(
				(session) -> session.find( EntityWithLocalTime.class, 1 )
		);
	}

	@Entity( name = "EntityWithLocalTime" )
	@Table( name = "EntityWithLocalTime" )
	public static class EntityWithLocalTime {
		@Id
		private Integer id;

		@Column( name = "`localTime`")
		//tag::basic-localTime-example[]
		// mapped as TIME
		private LocalTime localTime;
		//end::basic-localTime-example[]

		public EntityWithLocalTime() {
		}

		public EntityWithLocalTime(Integer id, LocalTime localTime) {
			this.id = id;
			this.localTime = localTime;
		}
	}
}
