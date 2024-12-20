package org.hibernate.processor.test.typeliteral;

import java.math.BigDecimal;

import jakarta.persistence.Entity;

@Entity(name = "Debit")
public class DebitAccount extends Account {
	private BigDecimal overdraftFee;
//Getters and setters are omitted for brevity
}