package org.hibernate.envers.test.integration.manytoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class OneToManyOwned implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@OneToMany(mappedBy = "references")
	private Set<ManyToOneOwning> referencing = new HashSet<ManyToOneOwning>();

	public OneToManyOwned() {
	}

	public OneToManyOwned(String data, Set<ManyToOneOwning> referencing) {
		this.data = data;
		this.referencing = referencing;
	}

	public OneToManyOwned(String data, Set<ManyToOneOwning> referencing, Long id) {
		this.id = id;
		this.data = data;
		this.referencing = referencing;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof OneToManyOwned) ) {
			return false;
		}

		OneToManyOwned that = (OneToManyOwned) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "OneToManyOwned(id = " + id + ", data = " + data + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Set<ManyToOneOwning> getReferencing() {
		return referencing;
	}

	public void setReferencing(Set<ManyToOneOwning> referencing) {
		this.referencing = referencing;
	}
}
