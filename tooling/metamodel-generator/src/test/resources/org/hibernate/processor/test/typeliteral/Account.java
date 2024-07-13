package org.hibernate.processor.test.typeliteral;

import java.math.BigDecimal;

import org.hibernate.annotations.processing.CheckHQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;

@Entity(name = "Account")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@CheckHQL
@NamedQuery(name = "#creditAccounts", query = "select a from Account a where type(a) = Credit")
@NamedQuery(name = "#debitAccounts", query = "select a from Account a where type(a) = org.hibernate.processor.test.typeliteral.DebitAccount")
public class Account {
	@Id
	private Long id;
	private String owner;
	private BigDecimal balance;
	private BigDecimal interestRate;
//Getters and setters are omitted for brevity
}