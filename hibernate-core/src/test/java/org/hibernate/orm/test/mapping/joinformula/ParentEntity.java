package org.hibernate.orm.test.mapping.joinformula;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class ParentEntity {

	@Id
	private Long id;

	@OneToOne(targetEntity = ChildEntity.class, optional = false)
	@JoinColumnOrFormula(column = @JoinColumn(name = "id", referencedColumnName = "PARENT_ID", insertable = false, updatable = false))
	@JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "NAME", value = "'Tom'"))
	private ChildEntity tom;

	@OneToOne(targetEntity = ChildEntity.class, optional = false)
	@JoinColumnOrFormula(column = @JoinColumn(name = "id", referencedColumnName = "PARENT_ID", insertable = false, updatable = false))
	@JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "NAME", value = "'Ben'"))
	private ChildEntity ben;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ChildEntity getTom() {
		return tom;
	}

	public ChildEntity getBen() {
		return ben;
	}
}
