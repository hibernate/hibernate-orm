/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.mapping.type.java.YearMappingTests;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.CurrencyJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.time.Year;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = CurrencyMappingTests.EntityWithCurrency.class)
@SessionFactory
@JiraKey("HHH-17574")
public class CurrencyMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor(EntityWithCurrency.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("currency");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Currency.class));
		assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));

		final EntityWithCurrency entity = createEntityWithCurrency();

		scope.inTransaction(
			(session) -> session.persist(entity)
		);

		scope.inTransaction(
			(session) -> session.find(EntityWithCurrency.class, 1)
		);
	}

	@Test
	public void basicAssertions(final SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final JdbcTypeRegistry jdbcTypeRegistry = sessionFactory.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor(
				EntityWithCurrency.class );
		{
			final BasicAttributeMapping currencyAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
					"currency");
			Assertions.assertThat(currencyAttribute.getJdbcMapping().getJdbcType())
					.isEqualTo(jdbcTypeRegistry.getDescriptor(Types.VARCHAR));
			Assertions.assertThat(currencyAttribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass()).isEqualTo(
					Currency.class );
		}
		{
			final PluralAttributeMapping currenciesAttribute = (PluralAttributeMapping) entityDescriptor.findAttributeMapping(
					"currencies" );
			final BasicValuedCollectionPart elementDescriptor = (BasicValuedCollectionPart) currenciesAttribute.getElementDescriptor();
			Assertions.assertThat( elementDescriptor.getJdbcMapping().getJdbcType() )
					.isEqualTo( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ) );
			Assertions.assertThat( elementDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo(
					Currency.class );
		}
	}

	@Test
	public void testUnwrapPass() {
		final CurrencyJavaType currencyJavaType = new CurrencyJavaType();
		final Currency currency = Currency.getInstance("CHF");
		{
			final Currency c = currencyJavaType.unwrap(currency, Currency.class, null);
			Assertions.assertThat( c ).isEqualTo( currency );
		}
		{
			final String c = currencyJavaType.unwrap(currency, String.class, null);
			Assertions.assertThat( c ).isEqualTo( "CHF" );
		}
	}

	@Test
	public void testUnwrapFail() {
		final CurrencyJavaType currencyJavaType = new CurrencyJavaType();
		final Currency currency = Currency.getInstance("CHF");
		{
			Assertions.assertThatThrownBy( () ->
				currencyJavaType.unwrap(currency, Boolean.class, null)
			).isInstanceOf( HibernateException.class );
		}
	}

	@Test
	public void testWrapPass() {
		final CurrencyJavaType currencyJavaType = new CurrencyJavaType();
		{
			final Currency usingNull = currencyJavaType.wrap(null, null);
			Assertions.assertThat(usingNull).isNull();
		}
		{
			final Currency usingString = currencyJavaType.wrap("CHF", null);
			Assertions.assertThat(usingString).isNotNull();
		}
		{
			final Currency usingCurrency = currencyJavaType.wrap(Currency.getInstance("CHF"), null);
			Assertions.assertThat(usingCurrency).isNotNull();
		}
	}

	@Test
	public void testWrapFail() {
		final CurrencyJavaType currencyJavaType = new CurrencyJavaType();
		{
			final String usingEmptyString = "";
			Assertions.assertThatThrownBy(() ->
				currencyJavaType.wrap(usingEmptyString, null)
			).isInstanceOf(IllegalArgumentException.class);
		}
		{
			final Integer usingInteger = Integer.valueOf(269);
			Assertions.assertThatThrownBy(() ->
				currencyJavaType.wrap(usingInteger, null)
			).isInstanceOf(HibernateException.class);
		}
		{
			final CurrencyJavaType usingSelf = new CurrencyJavaType();
			Assertions.assertThatThrownBy(() ->
				currencyJavaType.wrap(usingSelf, null)
			).isInstanceOf(HibernateException.class);
		}
	}

	@Test
	public void testUsage(final SessionFactoryScope scope) {
		final EntityWithCurrency entity = createEntityWithCurrency();
		scope.inTransaction((session) -> session.persist(entity));
		try {
			scope.inTransaction(session -> session.createQuery("from EntityWithCurrency", EntityWithCurrency.class).list());
		}
		finally {
			scope.inTransaction( session -> session.remove( entity ) );
		}
	}

	private static EntityWithCurrency createEntityWithCurrency() {
		final Currency currency = Currency.getInstance("USD");

		final Set<Currency> currencies = new HashSet<>();
		currencies.add(Currency.getInstance("CHF"));
		currencies.add(Currency.getInstance("EUR"));

		return new EntityWithCurrency( 1, currency, currencies );
	}

	@Entity(name = "EntityWithCurrency")
	@Table(name = "EntityWithCurrency")
	public static class EntityWithCurrency {
		@Id
		private Integer id;

		//tag::basic-Currency-example[]
		// mapped as VARCHAR
		private Currency currency;
		//end::basic-Currency-example[]

		@ElementCollection
		private Set<Currency> currencies;

        public EntityWithCurrency() {
			//
		}

		public EntityWithCurrency(final Integer id, final Currency currency, final Set<Currency> currencies) {
			this.id         = id;
			this.currency   = currency;
			this.currencies = currencies;
        }
	}

}
