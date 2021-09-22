/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.util.Currency;
import java.util.Locale;
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
@DomainModel( annotatedClasses = LocaleMappingTests.EntityWithLocale.class )
@SessionFactory
public class LocaleMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityWithLocale.class );

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "locale" );
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( Locale.class ) );
		assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.VARCHAR ) );

		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithLocale( 1, Locale.US ) );
				}
		);

		scope.inTransaction(
				(session) -> session.find( EntityWithLocale.class, 1 )
		);
	}

	@Entity( name = "EntityWithLocale" )
	@Table( name = "EntityWithLocale" )
	public static class EntityWithLocale {
		@Id
		private Integer id;

		//tag::basic-Locale-example[]
		// mapped as VARCHAR
		private Locale locale;
		//end::basic-Locale-example[]

		public EntityWithLocale() {
		}

		public EntityWithLocale(Integer id, Locale locale) {
			this.id = id;
			this.locale = locale;
		}
	}
}
