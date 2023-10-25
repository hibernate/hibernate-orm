package org.hibernate.test.annotations.delete.keepreference;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

@Entity
@SQLDelete(sql = "UPDATE universe SET deleted = true WHERE id = ?", keepReference = true)
@Where(clause = "deleted = false")
public class Universe extends BaseEntity {

	@OneToOne(optional = true, fetch = FetchType.EAGER, mappedBy = "universe")
	DeathStar deathStar;

	public DeathStar getDeathStar() {
		return deathStar;
	}

	public void setDeathStar(DeathStar deathStar) {
		this.deathStar = deathStar;
	}
}
