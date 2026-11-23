package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		JoinedInheritanceDiscriminatorInEmbeddedIdTest.PaymentParam.class,
		JoinedInheritanceDiscriminatorInEmbeddedIdTest.MyCompositeId.class,
		JoinedInheritanceDiscriminatorInEmbeddedIdTest.CCPaymentParam.class,
		JoinedInheritanceDiscriminatorInEmbeddedIdTest.BACSPaymentParam.class,
})
@Jira("https://hibernate.atlassian.net/browse/HHH-18910")
@Jira("https://hibernate.atlassian.net/browse/HHH-17020")
public class JoinedInheritanceDiscriminatorInEmbeddedIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testPersistAndQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CCPaymentParam cc = new CCPaymentParam();
			cc.setCountryCode( "GB" );
			cc.setPaymentMethod( PaymentMethod.CC );
			cc.setSomeColumn( "cc_value" );
			session.persist( cc );
			final BACSPaymentParam bacs = new BACSPaymentParam();
			bacs.setCountryCode( "US" );
			bacs.setPaymentMethod( PaymentMethod.BACS );
			bacs.setAnotherColumn( "bacs_value" );
			session.persist( bacs );
		} );
		scope.inTransaction( session -> {
			final CCPaymentParam cc = session.createQuery(
					"from CCPaymentParam",
					CCPaymentParam.class
			).getSingleResult();
			assertThat( cc.getSomeColumn() ).isEqualTo( "cc_value" );
			final BACSPaymentParam bacs = session.createQuery(
					"from BACSPaymentParam",
					BACSPaymentParam.class
			).getSingleResult();
			assertThat( bacs.getAnotherColumn() ).isEqualTo( "bacs_value" );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CCPaymentParam cc = new CCPaymentParam();
			cc.setCountryCode( "GB" );
			cc.setPaymentMethod( PaymentMethod.CC );
			cc.setSomeColumn( "cc_value" );
			session.persist( cc );
		} );
		scope.inTransaction( session -> {
			final PaymentParam result = session.createQuery(
					"from PaymentParam where id.countryCode = :cc",
					PaymentParam.class
			).setParameter( "cc", "GB" ).getSingleResult();
			assertThat( result ).isInstanceOf( CCPaymentParam.class );
			assertThat( ((CCPaymentParam) result).getSomeColumn() ).isEqualTo( "cc_value" );
		} );
	}

	public enum PaymentMethod {
		CC, BACS
	}

	@Embeddable
	public static class MyCompositeId implements Serializable {

		@Column(name = "country_code")
		private String countryCode;

		@Enumerated(EnumType.STRING)
		@Column(name = "payment_method")
		private PaymentMethod paymentMethod;

		public MyCompositeId() {
		}

		public String getCountryCode() {
			return countryCode;
		}

		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		public PaymentMethod getPaymentMethod() {
			return paymentMethod;
		}

		public void setPaymentMethod(PaymentMethod paymentMethod) {
			this.paymentMethod = paymentMethod;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !(o instanceof MyCompositeId that) ) {
				return false;
			}
			return Objects.equals( countryCode, that.countryCode )
				&& paymentMethod == that.paymentMethod;
		}

		@Override
		public int hashCode() {
			return Objects.hash( countryCode, paymentMethod );
		}
	}

	@Entity(name = "PaymentParam")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "payment_method")
	@DiscriminatorOptions(insert = false)
	@Table(name = "parent_payment_params")
	public static abstract class PaymentParam {

		@EmbeddedId
		private MyCompositeId id = new MyCompositeId();

		public MyCompositeId getId() {
			return id;
		}

		public void setCountryCode(String countryCode) {
			this.id.setCountryCode( countryCode );
		}

		public void setPaymentMethod(PaymentMethod paymentMethod) {
			this.id.setPaymentMethod( paymentMethod );
		}
	}

	@Entity(name = "CCPaymentParam")
	@Table(name = "payment_param_cc")
	@DiscriminatorValue("CC")
	public static class CCPaymentParam extends PaymentParam {

		@Column(name = "some_column")
		private String someColumn;

		public String getSomeColumn() {
			return someColumn;
		}

		public void setSomeColumn(String someColumn) {
			this.someColumn = someColumn;
		}
	}

	@Entity(name = "BACSPaymentParam")
	@Table(name = "payment_param_bacs")
	@DiscriminatorValue("BACS")
	public static class BACSPaymentParam extends PaymentParam {

		@Column(name = "another_column")
		private String anotherColumn;

		public String getAnotherColumn() {
			return anotherColumn;
		}

		public void setAnotherColumn(String anotherColumn) {
			this.anotherColumn = anotherColumn;
		}
	}
}
