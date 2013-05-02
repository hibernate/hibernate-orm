package org.hibernate.envers.test.integration.notinsertable.manytoone;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

@Entity
@Table(name = "ManyToOneNotIns")
@Audited
public class ManyToOneNotInsertableEntity {
	@Id
	private Integer id;

	@Basic
	@Column(name = "numVal")
	private Integer number;

	@ManyToOne
	@JoinColumn(name = "numVal", insertable = false, updatable = false)
	private NotInsertableEntityType type;

	public ManyToOneNotInsertableEntity() {
	}

	public ManyToOneNotInsertableEntity(Integer id, Integer number, NotInsertableEntityType type) {
		this.id = id;
		this.number = number;
		this.type = type;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public NotInsertableEntityType getType() {
		return type;
	}

	public void setType(NotInsertableEntityType type) {
		this.type = type;
	}
}
