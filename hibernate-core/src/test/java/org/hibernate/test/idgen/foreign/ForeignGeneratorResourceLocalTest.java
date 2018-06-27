/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.foreign;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12738")
public class ForeignGeneratorResourceLocalTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Contract.class,
				Customer.class,
				CustomerContractRelation.class
		};
	}

	@Test
	public void addRelationImplicitFlush() throws Exception {

		Long contractId = doInJPA( this::entityManagerFactory, entityManager -> {
			Contract contract = new Contract();

			entityManager.persist( contract );
			return contract.getId();
		} );

		Long customerId = doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();

			entityManager.persist( customer );
			return customer.getId();
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

			Customer customer = entityManager.createQuery(
					"SELECT c " +
							"FROM Customer c " +
							" LEFT JOIN FETCH c.contractRelations " +
							" WHERE c.id = :customerId", Customer.class )
					.setParameter( "customerId", customerId )
					.getSingleResult();

			CustomerContractRelation relation = new CustomerContractRelation();
			relation.setContractId( contractId );
			customer.addContractRelation( relation );
		} );
	}

	@Test
	public void addRelationExplicitFlush() throws Exception {
		Long contractId = doInJPA( this::entityManagerFactory, entityManager -> {
			Contract contract = new Contract();

			entityManager.persist( contract );
			return contract.getId();
		} );

		Long customerId = doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();

			entityManager.persist( customer );
			return customer.getId();
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

			Customer customer = entityManager.createQuery(
					"SELECT c " +
							"FROM Customer c " +
							" LEFT JOIN FETCH c.contractRelations " +
							" WHERE c.id = :customerId", Customer.class )
					.setParameter( "customerId", customerId )
					.getSingleResult();

			CustomerContractRelation relation = new CustomerContractRelation();
			relation.setContractId( contractId );
			customer.addContractRelation( relation );

			entityManager.flush();
		} );
	}

	@Test
	public void addRelationImplicitFlushOneTx() throws Exception {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Contract contract = new Contract();

			entityManager.persist( contract );

			Customer customer = new Customer();

			entityManager.persist( customer );

			customer = entityManager.createQuery(
					"SELECT c " +
							"FROM Customer c " +
							" LEFT JOIN FETCH c.contractRelations " +
							" WHERE c.id = :customerId", Customer.class )
					.setParameter( "customerId", customer.getId() )
					.getSingleResult();

			CustomerContractRelation relation = new CustomerContractRelation();
			relation.setContractId( customer.getId() );
			customer.addContractRelation( relation );
		} );
	}

	@Test
	public void addRelationImplicitFlushCloseEntityManager() throws Exception {

		Long contractId = doInJPA( this::entityManagerFactory, entityManager -> {
			Contract contract = new Contract();

			entityManager.persist( contract );
			return contract.getId();
		} );

		Long customerId = doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();

			entityManager.persist( customer );
			return customer.getId();
		} );

		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();
			txn = entityManager.getTransaction();
			txn.begin();

			Customer customer = entityManager.createQuery(
					"SELECT c " +
							"FROM Customer c " +
							" LEFT JOIN FETCH c.contractRelations " +
							" WHERE c.id = :customerId", Customer.class )
					.setParameter( "customerId", customerId )
					.getSingleResult();

			CustomerContractRelation relation = new CustomerContractRelation();
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
