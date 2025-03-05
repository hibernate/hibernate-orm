/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.foreign;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12738")
@Jpa(
		annotatedClasses = {
				ForeignGeneratorResourceLocalTest.Contract.class,
				ForeignGeneratorResourceLocalTest.Customer.class,
				ForeignGeneratorResourceLocalTest.CustomerContractRelation.class
		}
)
public class ForeignGeneratorResourceLocalTest {
	private static final Logger log = Logger.getLogger( ForeignGeneratorResourceLocalTest.class );

	@Test
	public void baseline(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> {
					final Contract contract = new Contract();
					entityManager.persist( contract );

					final Customer customer = new Customer();
					entityManager.persist( customer );

					final CustomerContractRelation relation = new CustomerContractRelation();
					relation.setContractId( customer.getId() );
					customer.addContractRelation( relation );
				}
		);
	}

	@Test
	public void addRelationImplicitFlush(EntityManagerFactoryScope scope) {
		doIt( scope, false );
	}

	private void doIt(EntityManagerFactoryScope scope, boolean explicitFlush) {
		final Long contractId = scope.fromTransaction(
				(entityManager) -> {
					Contract contract = new Contract();

					entityManager.persist( contract );
					return contract.getId();
				}
		);

		final Long customerId = scope.fromTransaction(
				(entityManager) -> {
					Customer customer = new Customer();

					entityManager.persist( customer );
					return customer.getId();
				}
		);

		scope.inTransaction(
				(entityManager) -> {
					final String qry = "SELECT c " +
							"FROM Customer c " +
							" LEFT JOIN FETCH c.contractRelations " +
							" WHERE c.id = :customerId";
					final Customer customer = entityManager.createQuery( qry, Customer.class )
							.setParameter( "customerId", customerId )
							.getSingleResult();

					final CustomerContractRelation relation = new CustomerContractRelation();
					relation.setContractId( contractId );
					customer.addContractRelation( relation );

					if ( explicitFlush ) {
						entityManager.flush();
					}
				}
		);
	}

	@Test
	public void addRelationExplicitFlush(EntityManagerFactoryScope scope) {
		doIt( scope, true );
	}

	@Test
	public void addRelationImplicitFlushCloseEntityManager(EntityManagerFactoryScope scope) {
		final Long contractId = scope.fromTransaction(
				(entityManager) -> {
					Contract contract = new Contract();

					entityManager.persist( contract );
					return contract.getId();
				}
		);

		final Long customerId = scope.fromTransaction(
				(entityManager) -> {
					Customer customer = new Customer();

					entityManager.persist( customer );
					return customer.getId();
				}
		);

		EntityManager entityManager = null;
		EntityTransaction txn = null;

		try {
			entityManager = scope.getEntityManagerFactory().createEntityManager();
			txn = entityManager.getTransaction();
			txn.begin();

			final Customer customer = entityManager.createQuery(
					"SELECT c " +
							"FROM Customer c " +
							" LEFT JOIN FETCH c.contractRelations " +
							" WHERE c.id = :customerId", Customer.class )
					.setParameter( "customerId", customerId )
					.getSingleResult();

			final CustomerContractRelation relation = new CustomerContractRelation();
			relation.setContractId( contractId );
			customer.addContractRelation( relation );

			//Close the EntityManager
			entityManager.close();

			//And, afterward commit the currently running Tx.
			//This might happen in JTA environments where the Tx is committed by the JTA TM.
			txn.commit();
		}
		catch (Throwable t) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
	}

	@Entity(name = "Contract")
	@Table(name = "CONTRACT")
	public static class Contract {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER")
	public static class Customer {
		@OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE },
				orphanRemoval = true, targetEntity = CustomerContractRelation.class)
		private final Set<CustomerContractRelation> contractRelations = new HashSet<>();
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<CustomerContractRelation> getContractRelations() {
			return contractRelations;
		}

		public boolean addContractRelation(CustomerContractRelation relation) {
			if ( relation != null ) {
				relation.setCustomer( this );
				return contractRelations.add( relation );
			}
			return false;
		}
	}

	@Embeddable
	public static class CustomerContractId implements Serializable {
		private static final long serialVersionUID = 1115591676841551563L;

		@Column(name = "CUSTOMERID", nullable = false)
		private Long customerId;

		@Column(name = "CONTRACTID", nullable = false)
		private Long contractId;

		public Long getCustomerId() {
			return customerId;
		}

		public void setCustomerId(Long customerId) {
			this.customerId = customerId;
		}

		public Long getContractId() {
			return contractId;
		}

		public void setContractId(Long contractId) {
			this.contractId = contractId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( contractId == null ) ? 0 : contractId.hashCode() );
			result = prime * result + ( ( customerId == null ) ? 0 : customerId.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			CustomerContractId other = (CustomerContractId) obj;
			if ( contractId == null ) {
				if ( other.contractId != null ) {
					return false;
				}
			}
			else if ( !contractId.equals( other.contractId ) ) {
				return false;
			}
			if ( customerId == null ) {
				if ( other.customerId != null ) {
					return false;
				}
			}
			else if ( !customerId.equals( other.customerId ) ) {
				return false;
			}
			return true;
		}
	}

	@Entity(name = "CustomerContractRelation")
	@Table(name = "CUSTOMER_CONTRACT_RELATION")
	public static class CustomerContractRelation {
		@EmbeddedId
		private final CustomerContractId id = new CustomerContractId();

		@Temporal(TemporalType.TIMESTAMP)
		@Column(nullable = true, name = "SIGNEDONDATE")
		private Date signedOn;

		@MapsId(value = "customerId")
		@JoinColumn(name = "CUSTOMERID", nullable = false)
		@ManyToOne(fetch = FetchType.LAZY)
		private Customer customer;

		public CustomerContractId getId() {
			return id;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public Date getSignedOn() {
			return signedOn;
		}

		public void setSignedOn(Date signedOn) {
			this.signedOn = signedOn;
		}

		public Long getCustomerId() {
			return id.getCustomerId();
		}

		public void setCustomerId(Long customerId) {
			id.setCustomerId( customerId );
		}

		public Long getContractId() {
			return id.getContractId();
		}

		public void setContractId(Long contractId) {
			id.setContractId( contractId );
		}
	}
}
