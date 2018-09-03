package org.hibernate.test.annotations.override.mappedsuperclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity
@AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
public class SubclassWithUuidAsId extends MappedSuperClassWithUuidAsBasic {

	@Id
	@Access(AccessType.PROPERTY)
	@Override
	public Long getUid() {
		return super.getUid();
	}
}
