//$Id$
package org.hibernate.test.annotations.id.entities;
import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

/**
 * Blown precision on related entity when &#064;JoinColumn is used. 
 * Does not cause an issue on HyperSonic, but replicates nicely on PGSQL.
 * 
 * @see ANN-748
 * @author Andrew C. Oliver andyspam@osintegrators.com
 */
@Entity
@SuppressWarnings("serial")
public class PointyTooth implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "java5_uuid")
	@GenericGenerator(name = "java5_uuid", strategy = "org.hibernate.test.annotations.id.UUIDGenerator")
	@Column(name = "id", precision = 128, scale = 0)
	private BigInteger id;

	@ManyToOne
    @JoinColumn(name = "bunny_id")
	Bunny bunny;

	public void setBunny(Bunny bunny) {
		this.bunny = bunny;
	}

	public BigInteger getId() {
		return id;
	}
}
