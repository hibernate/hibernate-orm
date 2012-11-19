package org.hibernate.envers.test.integration.inheritance.tableperclass.abstractparent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Audited
public abstract class AbstractEntity {
	@Id
	public Long id;

	@Column
	public String commonField;

	public AbstractEntity() {
	}

	protected AbstractEntity(Long id, String commonField) {
		this.commonField = commonField;
		this.id = id;
	}
}
