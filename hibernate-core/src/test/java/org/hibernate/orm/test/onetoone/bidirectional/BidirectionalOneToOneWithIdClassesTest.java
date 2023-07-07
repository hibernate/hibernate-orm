/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-16908" )
public class BidirectionalOneToOneWithIdClassesTest {

	@Test
	public void test() {
		StandardServiceRegistry ssr = null;
		try {
			ssr = new StandardServiceRegistryBuilder( new BootstrapServiceRegistryBuilder().build() ).build();

			new MetadataSources( ssr )
					.addAnnotatedClasses( SpecialPricePoint.class, SpecialProduct.class, SpecialOperator.class )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
		}
		finally {
			if ( ssr != null ) {
				( (ServiceRegistryImplementor) ssr).destroy();
			}
		}

	}

	public enum Provider {
		A,
		B
	}

	@Entity
	@IdClass(SpecialOperatorPK.class)
	@Table(name = "SPECIAL_OPERATORS")
	public class SpecialOperator {

		@Id
		private Provider provider;

		@Id
		private String operatorId;

		@OneToMany(mappedBy = "operator")
		private final Set<SpecialPricePoint> pricePoints = new HashSet<>();

		public SpecialOperator(Provider provider, String operatorId) {
			this.provider = provider;
			this.operatorId = operatorId;
		}
	}

	@Embeddable
	public static class SpecialOperatorPK implements Serializable {
		@Enumerated(EnumType.STRING)
		@Column(name = "PROVIDER_ID")
		Provider provider;

		@Column(name = "OPERATOR_ID")
		String operatorId;
	}

	@Entity
	@Table(name = "SPECIAL_OPERATOR_PRICES_POINTS")
	@IdClass(SpecialPricePointPK.class)
	public class SpecialPricePoint {

		public SpecialPricePoint( SpecialOperator operator, String wholesalePrice) {
			this.operator = operator;
			this.wholesalePrice = wholesalePrice;
		}

		@ManyToOne
		@JoinColumn(name = "PROVIDER_ID", referencedColumnName = "PROVIDER_ID")
		@JoinColumn(name = "OPERATOR_ID", referencedColumnName = "OPERATOR_ID")
		@MapsId
		private SpecialOperator operator;

		@Column(name = "PRODUCT_ID")
		private String productId;

		@Id
		@Column(name = "PRICE_POINT")
		String wholesalePrice;

		@OneToOne
		@JoinColumn(name = "PRODUCT_ID", referencedColumnName = "PRODUCT_ID", updatable = false, insertable = false)
		@JoinColumn(name = "PRICE_POINT", referencedColumnName = "WHOLESALE_PRICE_AMOUNT", updatable = false, insertable = false)
		@JoinColumn(name = "OPERATOR_ID", referencedColumnName = "OPERATOR_ID", updatable = false, insertable = false)
		@JoinColumn(name = "PROVIDER_ID", referencedColumnName = "PROVIDER_ID", updatable = false, insertable = false)
		private SpecialProduct product;
	}

	@Embeddable
	public static class SpecialPricePointPK implements Serializable {
		@Embedded
		@AttributeOverride(name = "provider", column = @Column(name = "PROVIDER_ID"))
		@AttributeOverride(name = "operatorId", column = @Column(name = "OPERATOR_ID"))
		SpecialOperatorPK operator;
		String wholesalePrice;
	}

	@Table(name = "SPECIAL_PRODUCTS")
	@Entity
	@IdClass(SpecialProductPK.class)
	public class SpecialProduct {
		public SpecialProduct(String productId, SpecialPricePoint wholesalePrice) {
			this.productId = productId;
			this.wholesalePrice = wholesalePrice;
		}

		@Id
		@Column(name = "PRODUCT_ID")
		private String productId;

		@OneToOne(optional = false, mappedBy = "product")
		@MapsId
		private SpecialPricePoint wholesalePrice;
	}

	@Embeddable
	public static class SpecialProductPK implements Serializable {
		@Embedded
		@AttributeOverride(name = "operator.provider", column = @Column(name = "PROVIDER_ID"))
		@AttributeOverride(name = "operator.operatorId", column = @Column(name = "OPERATOR_ID"))
		@AttributeOverride(name = "wholesalePrice", column = @Column(name = "WHOLESALE_PRICE_AMOUNT"))
		SpecialPricePointPK wholesalePrice;
		String productId;
	}
}
