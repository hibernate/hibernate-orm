/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags.naming;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited(withModifiedFlag = true)
public class OtherEntity {
	@EmbeddedId
	private OtherEntityId id;

	@Column(name = "d")
	private String data;

	public OtherEntityId getId() {
		return id;
	}

	public void setId(OtherEntityId id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
