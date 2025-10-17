/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-1134" )
public class MultiInheritanceDiscriminatorTest {

	@DomainModel(
			xmlMappings = "org/hibernate/orm/test/mapping/inheritance/discriminator/AccountOwner.hbm.xml"
	)
	@SessionFactory
	@Test
	public void testAbstractSuperClassMappingHbm(SessionFactoryScope scope) {
		AccountOwner owner = new AccountOwner();
		owner.setDescription( "Some account" );

		CreditAccount cAcc1 = new CreditAccount();
		cAcc1.setAmount( new BigDecimal( "123.34" ).setScale( 19, RoundingMode.DOWN ) );
		cAcc1.setOwner( owner );
		owner.getCreditAccounts().add( cAcc1 );

		CreditAccount cAcc2 = new CreditAccount();
		cAcc2.setAmount( new BigDecimal( "321.43" ).setScale( 19, RoundingMode.DOWN ) );
		cAcc2.setOwner( owner );
		owner.getCreditAccounts().add( cAcc2 );

		DebitAccount dAcc = new DebitAccount();
		dAcc.setAmount( new BigDecimal( "654.99" ).setScale( 19, RoundingMode.DOWN ) );
		dAcc.setOwner( owner );
		owner.getDebitAccounts().add( dAcc );

		scope.inTransaction(
				session ->
						session.persist( owner )
		);

		Long ownerId = owner.getId();

		scope.inTransaction(
				session -> {
					AccountOwner _owner = session.find( AccountOwner.class, ownerId );
					assertEquals( 2, _owner.getCreditAccounts().size() );
					assertEquals( "CreditAccount", _owner.getCreditAccounts().iterator().next().getClass().getSimpleName() );

					assertEquals( 1, _owner.getDebitAccounts().size() );
					assertEquals( "DebitAccount", _owner.getDebitAccounts().iterator().next().getClass().getSimpleName() );
				}
		);
	}

	@DomainModel(
			annotatedClasses = {
					AbstractAcc.class, CreditAcc.class, DebitAcc.class, AccOwner.class
			}
	)
	@SessionFactory
	@Test
	public void testAbstractSuperClassMappingAnn(SessionFactoryScope scope) {
		AccOwner owner = new AccOwner();
		owner.setName( "Some account" );

		CreditAcc cAcc1 = new CreditAcc();
		cAcc1.setAmount( new BigDecimal( "123.34" ).setScale( 19, RoundingMode.DOWN ) );
		cAcc1.setOwner( owner );
		owner.getCreditAccs().add( cAcc1 );

		CreditAcc cAcc2 = new CreditAcc();
		cAcc2.setAmount( new BigDecimal( "321.43" ).setScale( 19, RoundingMode.DOWN ) );
		cAcc2.setOwner( owner );
		owner.getCreditAccs().add( cAcc2 );

		DebitAcc dAcc = new DebitAcc();
		dAcc.setAmount( new BigDecimal( "654.99" ).setScale( 19, RoundingMode.DOWN ) );
		dAcc.setOwner( owner );
		owner.getDebitAccs().add( dAcc );

		scope.inTransaction(
				session ->
						session.persist( owner )
		);

		Long ownerId = owner.getId();

		scope.inTransaction(
				session -> {
					AccOwner _owner = session.find( AccOwner.class, ownerId );
					assertEquals( 2, _owner.getCreditAccs().size() );
					assertEquals( "CreditAcc", _owner.getCreditAccs().iterator().next().getClass().getSimpleName() );

					assertEquals( 1, _owner.getDebitAccs().size() );
					assertEquals( "DebitAcc", _owner.getDebitAccs().iterator().next().getClass().getSimpleName() );
				}
		);
	}

	@Entity
	@Table(name = "AccOwner")
	public static class AccOwner {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(targetEntity = CreditAcc.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
		private Set<AbstractAcc> creditAccs = new HashSet<>();

		@OneToMany(targetEntity = DebitAcc.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
		private Set<AbstractAcc> debitAccs = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<AbstractAcc> getCreditAccs() {
			return creditAccs;
		}

		public void setCreditAccs(Set<AbstractAcc> creditAccs) {
			this.creditAccs = creditAccs;
		}

		public Set<AbstractAcc> getDebitAccs() {
			return debitAccs;
		}

		public void setDebitAccs(Set<AbstractAcc> debitAccs) {
			this.debitAccs = debitAccs;
		}
	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "DISC")
	@Table(name = "Accounts")
	public static abstract class AbstractAcc {
		@Id
		@GeneratedValue
		private Long id;

		private BigDecimal amount;

		@ManyToOne
		private AccOwner owner;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public AccOwner getOwner() {
			return owner;
		}

		public void setOwner(AccOwner owner) {
			this.owner = owner;
		}
	}

	@Entity
	@DiscriminatorValue("CA")
	public static class CreditAcc extends AbstractAcc {
	}

	@Entity
	@DiscriminatorValue("DA")
	public static class DebitAcc extends AbstractAcc {
	}

}
