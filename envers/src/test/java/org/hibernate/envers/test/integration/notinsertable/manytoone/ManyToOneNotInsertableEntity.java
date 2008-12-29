package org.hibernate.envers.test.integration.notinsertable.manytoone;

import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Audited
public class ManyToOneNotInsertableEntity {
    @Id
    private Integer id;

    @Basic
	@Column(name = "number")
    private Integer number;

	@ManyToOne
	@JoinColumn(name = "number", insertable = false, updatable = false)
	private NotInsertableEntityType type;

	public ManyToOneNotInsertableEntity() { }

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
