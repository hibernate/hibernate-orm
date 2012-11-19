package org.hibernate.envers.test.integration.inheritance.tableperclass.abstractparent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "ENTITY_1")
@Audited
public class EffectiveEntity1 extends AbstractEntity {
	@Column
	public String specificField1;

	public EffectiveEntity1() {
	}

	public EffectiveEntity1(Long id, String commonField, String specificField1) {
		super( id, commonField );
		this.specificField1 = specificField1;
	}
}
