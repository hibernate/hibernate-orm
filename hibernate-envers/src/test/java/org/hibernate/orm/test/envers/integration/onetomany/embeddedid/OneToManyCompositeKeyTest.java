/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.embeddedid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11770")
public class OneToManyCompositeKeyTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Contract.class, Design.class, DesignContract.class };
	}

	@Test
	public void initData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Contract contract = new Contract( 1 );
			final Design design = new Design( 1 );
			final DesignContract designContract = new DesignContract( contract, design );
			designContract.setGoal( 25d );
			contract.getDesigns().add( designContract );
			entityManager.persist( design );
			entityManager.persist( contract );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Contract.class, 1 ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Design.class, 1 ) );
	}

	@Test
	public void testOneToManyAssociationAuditQuery() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final AuditReader auditReader = getAuditReader();
			final Contract contract = auditReader.find( Contract.class, 1, 1 );
			final Design design = auditReader.find( Design.class, 1, 1 );
			assertEquals( 1, contract.getDesigns().size() );

			final DesignContract designContract = contract.getDesigns().iterator().next();
			assertEquals( contract.getId(), designContract.getContract().getId() );
			assertEquals( design.getId(), designContract.getDesign().getId() );
		} );
	}

	@Entity(name = "Contract")
	@Table(name = "CONTRACTS")
	@Audited
	public static class Contract {
		@Id
		private Integer id;

		@OneToMany(mappedBy = "pk.contract", fetch = FetchType.EAGER, cascade = { CascadeType.ALL } )
		@Fetch(value = FetchMode.SUBSELECT)
		private List<DesignContract> designs = new ArrayList<>();

		Contract() {

		}

		Contract(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<DesignContract> getDesigns() {
			return designs;
		}

		public void setDesigns(List<DesignContract> designs) {
			this.designs = designs;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Contract contract = (Contract) o;

			return id != null ? id.equals( contract.id ) : contract.id == null;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Contract{" +
					"id=" + id +
					'}';
		}
	}

	@Entity(name = "DesignContract")
	@AssociationOverrides(value = {
			@AssociationOverride(name = "pk.contract", joinColumns = @JoinColumn(name = "FK_CONTRACT")),
			@AssociationOverride(name = "pk.design", joinColumns = @JoinColumn(name = "FK_DESIGN"))
	})
	@Table(name = "CONTRACT_DESIGNS")
	@Audited
	public static class DesignContract {
		@EmbeddedId
		private DesignContractId pk = new DesignContractId();
		@Basic
		@Column(name = "GOAL", nullable = false, precision = 5)
		private Double goal;

		DesignContract() {

		}

		DesignContract(Contract contract, Design design) {
			pk.setContract( contract );
			pk.setDesign( design );
		}

		public DesignContractId getPk() {
			return pk;
		}

		public void setPk(DesignContractId pk) {
			this.pk = pk;
		}

		@Transient
		public Contract getContract() {
			return pk.getContract();
		}

		@Transient
		public Design getDesign() {
			return pk.getDesign();
		}

		public Double getGoal() {
			return goal;
		}

		public void setGoal(Double goal) {
			this.goal = goal;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			DesignContract that = (DesignContract) o;

			if ( pk != null ? !pk.equals( that.pk ) : that.pk != null ) {
				return false;
			}
			return goal != null ? goal.equals( that.goal ) : that.goal == null;
		}

		@Override
		public int hashCode() {
			int result = pk != null ? pk.hashCode() : 0;
			result = 31 * result + ( goal != null ? goal.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			return "DesignContract{" +
					"pk=" + pk +
					", goal=" + goal +
					'}';
		}
	}

	@Embeddable
	public static class DesignContractId implements Serializable {
		@ManyToOne
		private Contract contract;
		@ManyToOne
		private Design design;

		DesignContractId() {

		}

		DesignContractId(Contract contract, Design design) {
			this.contract = contract;
			this.design = design;
		}

		public Contract getContract() {
			return contract;
		}

		public void setContract(Contract contract) {
			this.contract = contract;
		}

		public Design getDesign() {
			return design;
		}

		public void setDesign(Design design) {
			this.design = design;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			DesignContractId that = (DesignContractId) o;

			if ( contract != null ? !contract.equals( that.contract ) : that.contract != null ) {
				return false;
			}
			return design != null ? design.equals( that.design ) : that.design == null;
		}

		@Override
		public int hashCode() {
			int result = contract != null ? contract.hashCode() : 0;
			result = 31 * result + ( design != null ? design.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			return "DesignContractId{" +
					"contract=" + contract +
					", design=" + design +
					'}';
		}
	}

	@Entity(name = "Design")
	@Audited
	public static class Design {
		@Id
		private Integer id;

		Design() {

		}

		Design(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Design design = (Design) o;

			return id != null ? id.equals( design.id ) : design.id == null;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Design{" +
					"id=" + id +
					'}';
		}
	}
}
