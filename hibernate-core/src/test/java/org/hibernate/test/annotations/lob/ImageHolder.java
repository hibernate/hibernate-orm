//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
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
