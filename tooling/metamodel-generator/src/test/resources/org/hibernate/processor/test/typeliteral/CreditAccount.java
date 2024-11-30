package org.hibernate.processor.test.typeliteral;

import java.math.BigDecimal;

import jakarta.persistence.Entity;

@Entity(name = "Credit")
public class CreditAccount extends Account {
	private BigDecimal creditLimit;
//Getters and setters are omitted for brevity
}