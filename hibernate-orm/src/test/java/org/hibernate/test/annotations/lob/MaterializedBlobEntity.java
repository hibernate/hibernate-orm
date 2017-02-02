/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class MaterializedBlobEntity {
	@Id()
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	private String name;

	@Lob
	private byte[] theBytes;

	public MaterializedBlobEntity() {
	}

	public MaterializedBlobEntity(String name, byte[] theBytes) {
		this.name = name;
		this.theBytes = theBytes;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getTheBytes() {
		return theBytes;
	}

	public void setTheBytes(byte[] theBytes) {
		this.theBytes = theBytes;
	}
}
