/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
