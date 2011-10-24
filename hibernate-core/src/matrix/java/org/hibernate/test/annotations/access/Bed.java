//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.AccessType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AccessType("property")
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
