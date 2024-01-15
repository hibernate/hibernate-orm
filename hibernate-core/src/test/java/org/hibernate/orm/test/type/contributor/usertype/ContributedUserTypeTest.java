/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type.contributor.usertype;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.orm.test.mapping.basic.MonetaryAmount;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.type.UserComponentType;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				ContributedUserTypeTest.StringWrapperTestEntity.class,
				ContributedUserTypeTest.MyCompositeValueTestEntity.class,
				ContributedUserTypeTest.Wallet.class
		},
		typeContributors = { StringWrapperTypeContributor.class, MyCompositeValueTypeContributor.class }
)
@SessionFactory
@BootstrapServiceRegistry(
		javaServices = {
				@BootstrapServiceRegistry.JavaService( role = TypeContributor.class, impl = ServiceLoadedCustomUserTypeTypeContributor.class)
		}
)
public class ContributedUserTypeTest {

	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Wallet" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey( "HHH-14408" )
	public void test(SessionFactoryScope scope) {
		final Type type = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( StringWrapperTestEntity.class )
				.getPropertyType( "stringWrapper" );
		Assertions.assertTrue(
				type instanceof CustomType,
				"Type was initialized too early i.e. before type-contributors were run"
		);
	}

	@Test
	@JiraKey( "HHH-17181" )
	public void testComposite(SessionFactoryScope scope) {
		final Type type = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( MyCompositeValueTestEntity.class )
				.getPropertyType( "compositeValue" );
		Assertions.assertInstanceOf( UserComponentType.class, type );
	}

	@Test
	@JiraKey( "HHH-17100" )
	public void testParameter(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.createSelectionQuery( "from StringWrapperTestEntity e where e.stringWrapper = :p" )
							.setParameter( "p", new StringWrapper( "abc" ) )
							.getResultList();
				}
		);
	}

	@Test
	@JiraKey( "HHH-17181" )
	public void testCompositeParameter(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.createSelectionQuery( "from MyCompositeValueTestEntity e where e.compositeValue = :c" )
							.setParameter( "c", new MyCompositeValue( 1L, "1" ) )
							.getResultList();
				}
		);
	}

	@Test
	@Jira( value = "https://hibernate.atlassian.net/browse/HHH-17635" )
	public void testServiceLoadedCustomUserType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Wallet wallet = new Wallet();
					wallet.setId( 1L );
					wallet.setMoney( new MonetaryAmount( new BigDecimal( 1000 ), Currency.getInstance("EUR")) );
					session.persist( wallet );
				}
		);
		scope.inTransaction(
				session -> {
					Wallet w = session.createSelectionQuery( "from Wallet", Wallet.class ).getSingleResult();
					MonetaryAmount amount = w.getMoney();
					Assertions.assertNotNull( amount );
					Assertions.assertEquals( 1000, amount.getAmount().intValue() );
					Assertions.assertEquals( "EUR", amount.getCurrency().getCurrencyCode() );
				}
		);
	}

	@Entity( name = "StringWrapperTestEntity" )
	public static class StringWrapperTestEntity implements Serializable {
		@Id
		private Integer id;
		private StringWrapper stringWrapper;
	}

	@Entity( name = "MyCompositeValueTestEntity" )
	public static class MyCompositeValueTestEntity implements Serializable {
		@Id
		private Integer id;
		private MyCompositeValue compositeValue;
	}

	@Entity(name = "Wallet")
	public static class Wallet {

		@Id
		private Long id;

		private MonetaryAmount money;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MonetaryAmount getMoney() {
			return money;
		}

		public void setMoney(MonetaryAmount money) {
			this.money = money;
		}
	}

}
