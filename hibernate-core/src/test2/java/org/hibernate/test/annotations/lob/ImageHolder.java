/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.annotations.lob;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * An entity containing data that is materialized into a byte array immediately.
 * The hibernate type mapped for {@link #longByteArray} determines the SQL type
 * asctually used.
 * 
 * @author Gail Badner
 */
@Entity
public class ImageHolder {
	private Long id;
	private byte[] longByteArray;
	private Dog dog;
	private Byte[] picByteArray;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@org.hibernate.annotations.Type(type="image")
	public byte[] getLongByteArray() {
		return longByteArray;
	}

	public void setLongByteArray(byte[] longByteArray) {
		this.longByteArray = longByteArray;
	}
	@org.hibernate.annotations.Type(type="serializable_image")
	public Dog getDog() {
		return dog;
	}

	public void setDog(Dog dog) {
		this.dog = dog;
	}
	@org.hibernate.annotations.Type(type="wrapped_image")
	public Byte[] getPicByteArray() {
		return picByteArray;
	}

	public void setPicByteArray(Byte[] picByteArray) {
		this.picByteArray = picByteArray;
	}

}
