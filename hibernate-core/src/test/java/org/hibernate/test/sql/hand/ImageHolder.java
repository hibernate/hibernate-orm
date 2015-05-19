/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand;


/**
 * @author Gail Badner
 */
public class ImageHolder {
	private Long id;
	private byte[] photo;

	public ImageHolder(byte[] photo) {
		this.photo = photo;
	}

	public ImageHolder() {
	}

	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return Returns the photo.
	 */
	public byte[] getPhoto() {
		return photo;
	}

	/**
	 * @param photo The photo to set.
	 */
	public void setPhoto(byte[] photo) {
		this.photo = photo;
	}
}
