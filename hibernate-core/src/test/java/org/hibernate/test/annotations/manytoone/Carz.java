//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Carz implements Serializable {
	@Id
	private Integer id;

	@Column( name = "make", nullable = false )
	private String make;

	@Column( name = "model", nullable = false )
	private String model;

	@Column( name = "manufactured", nullable = false )
	@Temporal( TemporalType.TIMESTAMP )
	private Date manufactured;

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn( name = "loc_code", referencedColumnName = "loc_code" )
	private Lotz lot;

	public Carz() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Lotz getLot() {
		return this.lot;
	}

	public void setLot(Lotz lot) {
		this.lot = lot;
	}

	public String getMake() {
		return this.make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public Date getManufactured() {
		return this.manufactured;
	}

	public void setManufactured(Date manufactured) {
		this.manufactured = manufactured;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ( ( this.id == null ) ?
				0 :
				this.id.hashCode() );
		result = PRIME * result + ( ( this.make == null ) ?
				0 :
				this.make.hashCode() );
		result = PRIME * result + ( ( this.manufactured == null ) ?
				0 :
				this.manufactured.hashCode() );
		result = PRIME * result + ( ( this.model == null ) ?
				0 :
				this.model.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		final Carz other = (Carz) obj;
		if ( this.id == null ) {
			if ( other.id != null ) return false;
		}
		else if ( !this.id.equals( other.id ) ) return false;
		if ( this.make == null ) {
			if ( other.make != null ) return false;
		}
		else if ( !this.make.equals( other.make ) ) return false;
		if ( this.manufactured == null ) {
			if ( other.manufactured != null ) return false;
		}
		else if ( !this.manufactured.equals( other.manufactured ) ) return false;
		if ( this.model == null ) {
			if ( other.model != null ) return false;
		}
		else if ( !this.model.equals( other.model ) ) return false;
		return true;
	}
}
