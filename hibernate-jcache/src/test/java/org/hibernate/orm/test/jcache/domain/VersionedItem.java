package org.hibernate.orm.test.jcache.domain;

public class VersionedItem extends Item {
	private Long version;

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
