package org.hibernate.processor.test.typeliteral;

import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.processing.CheckHQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@CheckHQL
@NamedQuery(name = "#simple", query = "select s from Simple s where type(s) = Simple")
@NamedQuery(name = "#longer", query = "select s from Simple s where type(s) = org.hibernate.processor.test.typeliteral.Simple")
public class Simple {
	@Id
	private Integer id;
	private String value;
}
