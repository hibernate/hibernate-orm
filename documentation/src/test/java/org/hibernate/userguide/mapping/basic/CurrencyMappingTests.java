/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.util.Currency;
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
@DomainModel( annotatedClasses = CurrencyMappingTests.EntityWithCurrency.class )
@SessionFactory
public class CurrencyMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( EntityWithCurrency.class );

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "currency" );
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( Currency.class ) );
		assertThat( jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), equalTo( Types.VARCHAR ) );

		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithCurrency( 1, Currency.getInstance( "USD" ) ) );
				}
		);

		scope.inTransaction(
				(session) -> session.find( EntityWithCurrency.class, 1 )
		);
	}

	@Entity( name = "EntityWithCurrency" )
	@Table( name = "EntityWithCurrency" )
	public static class EntityWithCurrency {
		@Id
		private Integer id;

		//tag::basic-Currency-example[]
		// mapped as VARCHAR
		private Currency currency;
		//end::basic-Currency-example[]

		public EntityWithCurrency() {
		}

		public EntityWithCurrency(Integer id, Currency currency) {
			this.id = id;
			this.currency = currency;
		}
	}
}
