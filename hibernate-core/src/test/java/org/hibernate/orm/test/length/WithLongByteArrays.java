/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.length;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.Length.LOB_DEFAULT;
import static org.hibernate.Length.LONG;
import static org.hibernate.Length.LONG16;
import static org.hibernate.Length.LONG32;

@Entity
public class WithLongByteArrays {
	@Id
	@GeneratedValue
	public int id;

	@Column(length = LONG)
	public byte[] longish;

	@Column(length = LONG16)
	public byte[] long16;

	@Column(length = LONG32)
	public byte[] long32;

	@Column(length = LOB_DEFAULT+1)
	public byte[] lob;
}
