package org.hibernate.ejb.test.metadata;

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Thing {
	private Double weight;
	private Long version;

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	@Version
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
