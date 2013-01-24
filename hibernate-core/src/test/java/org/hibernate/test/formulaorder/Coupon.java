package org.hibernate.test.formulaorder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Formula;

@Entity
public class Coupon implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "coupon", cascade={CascadeType.PERSIST, CascadeType.MERGE})
	private List<Encasement> encasements = new ArrayList<Encasement>(0);

	@Formula("(SELECT DISTINCT TOP 1 e.ChequeNumber FROM Encasement e WHERE e.IdCoupon = Id)")
	private String chequeNumber;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Encasement> getEncasements() {
		return encasements;
	}

	public void setEncasements(List<Encasement> facEncaissements) {
		this.encasements = facEncaissements;
	}

	public String getChequeNumber() {
		return chequeNumber;
	}

	public void setChequeNumber(String chequeNumber) {
		this.chequeNumber = chequeNumber;
	}
}
