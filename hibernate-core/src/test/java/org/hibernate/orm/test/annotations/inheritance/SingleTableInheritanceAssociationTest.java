/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(
		annotatedClasses = {
				SingleTableInheritanceAssociationTest.Contract.class,
				SingleTableInheritanceAssociationTest.ContractTypeA.class,
				SingleTableInheritanceAssociationTest.ContractTypeB.class,
				SingleTableInheritanceAssociationTest.Annex.class,
				SingleTableInheritanceAssociationTest.AnnexTypeA.class,
				SingleTableInheritanceAssociationTest.AnnexTypeB.class,
				SingleTableInheritanceAssociationTest.Attachment.class,
		}
)
@JiraKey(value = "HHH-15969")
public class SingleTableInheritanceAssociationTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					// ContractTypeA <- AnnexTypeA <- Attachment
					ContractTypeA contractA = new ContractTypeA();
					entityManager.persist( contractA );
					AnnexTypeA annexA = new AnnexTypeA();
					annexA.setContract( contractA );
					entityManager.persist( annexA );
					Attachment attachmentA = new Attachment();
					attachmentA.setAnnex( annexA );
					entityManager.persist( attachmentA );
					// ContractTypeB <- AnnexTypeB <- Attachment
					ContractTypeB contractB = new ContractTypeB();
					entityManager.persist( contractB );
					AnnexTypeB annexB = new AnnexTypeB();
					annexB.setContract( contractB );
					entityManager.persist( annexB );
					Attachment attachmentB = new Attachment();
					attachmentB.setAnnex( annexB );
					entityManager.persist( attachmentB );
				}
		);
	}

	@Test
	public void testQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Attachment> attachments = entityManager
							.createQuery( "select a from Attachment a", Attachment.class )
							.getResultList();
					assertThat( attachments ).hasSize( 2 );
				}
		);
	}

	@Entity(name = "Contract")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Contract {
		@Id
		@GeneratedValue
		private Long id;
		@OneToMany(mappedBy = "contract")
		private Set<Attachment> attachments;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Attachment> getAttachments() {
			return attachments;
		}

		public void setAttachments(Set<Attachment> attachments) {
			this.attachments = attachments;
		}
	}

	@Entity(name = "ContractTypeA")
	public static class ContractTypeA extends Contract {
		@OneToMany(mappedBy = "contract")
		private Set<AnnexTypeA> annexes;

		public Set<AnnexTypeA> getAnnexes() {
			return annexes;
		}

		public void setAnnexes(Set<AnnexTypeA> annexes) {
			this.annexes = annexes;
		}
	}

	@Entity(name = "ContractTypeB")
	public static class ContractTypeB extends Contract {
		@OneToMany(mappedBy = "contract")
		private Set<AnnexTypeB> annexes;

		public Set<AnnexTypeB> getAnnexes() {
			return annexes;
		}

		public void setAnnexes(Set<AnnexTypeB> annexes) {
			this.annexes = annexes;
		}
	}

	@Entity(name = "Annex")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Annex {
		@Id
		@GeneratedValue
		private Long id;
		@OneToMany(mappedBy = "annex")
		private Set<Attachment> attachments;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Attachment> getAttachments() {
			return attachments;
		}

		public void setAttachments(Set<Attachment> attachments) {
			this.attachments = attachments;
		}
	}

	@Entity(name = "AnnexTypeA")
	public static class AnnexTypeA extends Annex {
		@ManyToOne
		private ContractTypeA contract;

		public ContractTypeA getContract() {
			return contract;
		}

		public void setContract(ContractTypeA contract) {
			this.contract = contract;
		}
	}

	@Entity(name = "AnnexTypeB")
	public static class AnnexTypeB extends Annex {
		@ManyToOne
		private ContractTypeB contract;

		public ContractTypeB getContract() {
			return contract;
		}

		public void setContract(ContractTypeB contract) {
			this.contract = contract;
		}
	}

	@Entity(name = "Attachment")
	public static class Attachment {
		@Id
		@GeneratedValue
		private Long id;
		@ManyToOne
		private Contract contract;
		@ManyToOne
		private Annex annex;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Contract getContract() {
			return contract;
		}

		public void setContract(Contract contract) {
			this.contract = contract;
		}

		public Annex getAnnex() {
			return annex;
		}

		public void setAnnex(Annex annex) {
			this.annex = annex;
		}
	}
}
