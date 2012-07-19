package org.hibernate.jpa.test.cascade;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Soldier2 implements Serializable {
	private Integer id;
	private String name;
	private Troop2 troop;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "troop_fk")
	public Troop2 getTroop() {
		return troop;
	}

	public void setTroop(Troop2 troop) {
		this.troop = troop;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Soldier2 ) ) return false;

		final Soldier2 soldier = (Soldier2) o;

		if ( !name.equals( soldier.name ) ) return false;

		return true;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
