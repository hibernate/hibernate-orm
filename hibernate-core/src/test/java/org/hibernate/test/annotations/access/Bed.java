//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Access(AccessType.PROPERTY)
public class Bed extends Furniture {
	String quality;

	@Transient
	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}
}
