/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

/**
 * An entity with multiple attributes of basic type for use in testing using those types/attributes
 * in queries.
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name="ENT_W_MANY_COLS")
public class MultiTypedBasicAttributesEntity {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	private Long id;
	private byte[] someBytes;
	private Byte[] someWrappedBytes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public byte[] getSomeBytes() {
		return someBytes;
	}

	public void setSomeBytes(byte[] someBytes) {
		this.someBytes = someBytes;
	}

	public Byte[] getSomeWrappedBytes() {
		return someWrappedBytes;
	}

	public void setSomeWrappedBytes(Byte[] someWrappedBytes) {
		this.someWrappedBytes = someWrappedBytes;
	}
}
