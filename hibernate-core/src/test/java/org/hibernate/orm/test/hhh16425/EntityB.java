package org.hibernate.orm.test.hhh16425;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "EntityB")
@Table(name = "entityb")
public class EntityB {
	@Id
	@Column(name = "id", nullable = false)
	private int id;
	@Column(name = "name")
	private String name;

	@Column(name = "amount")
	private Integer amount;

	public EntityB() {
	}

	public EntityB(int id, String name, Integer amount) {
		this.id = id;
		this.name = name;
		this.amount = amount;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}
}
