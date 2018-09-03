package org.hibernate.test.annotations.override.mappedsuperclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.MappedSuperclass;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public class MappedSuperClassWithUuidAsBasic {

	Long uid;

	public Long getUid() {
		return uid;
	}

	public void setUid(Long uid) {
		this.uid = uid;
	}
}
