package org.hibernate.test.annotations.onetoone.hhh4851;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "DeviceType", length = 1)
@DiscriminatorValue(value = "C")
public class Hardware extends BaseEntity {

	private Hardware parent = null;

	protected Hardware() {

	}

	public Hardware(Hardware parent) {
		this.parent = parent;

	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	public Hardware getParent() {
		return this.parent;
	}

	public void setParent(Hardware parent) {
		this.parent = parent;
	}

}
