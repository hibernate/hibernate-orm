//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Lotz implements Serializable {
	@EmbeddedId
	protected LotzPK lotPK;

	@Column( name = "name", nullable = false )
	private String name;

	@Column( name = "location", nullable = false )
	private String location;

	@OneToMany( mappedBy = "lot", fetch = FetchType.LAZY, cascade = CascadeType.ALL )
	private List<Carz> cars;

	public Lotz() {
	}

	public List<Carz> getCars() {
		return this.cars;
	}

	public void setCars(List<Carz> cars) {
		this.cars = cars;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public LotzPK getLotPK() {
		return this.lotPK;
	}

	public void setLotPK(LotzPK lotPK) {
		this.lotPK = lotPK;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ( ( this.location == null ) ?
				0 :
				this.location.hashCode() );
		result = PRIME * result + ( ( this.lotPK == null ) ?
				0 :
				this.lotPK.hashCode() );
		result = PRIME * result + ( ( this.name == null ) ?
				0 :
				this.name.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		final Lotz other = (Lotz) obj;
		if ( this.location == null ) {
			if ( other.location != null ) return false;
		}
		else if ( !this.location.equals( other.location ) ) return false;
		if ( this.lotPK == null ) {
			if ( other.lotPK != null ) return false;
		}
		else if ( !this.lotPK.equals( other.lotPK ) ) return false;
		if ( this.name == null ) {
			if ( other.name != null ) return false;
		}
		else if ( !this.name.equals( other.name ) ) return false;
		return true;
	}
}
