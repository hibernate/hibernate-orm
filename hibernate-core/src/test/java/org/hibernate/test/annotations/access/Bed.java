//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.AttributeAccessor;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AttributeAccessor("property")
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
