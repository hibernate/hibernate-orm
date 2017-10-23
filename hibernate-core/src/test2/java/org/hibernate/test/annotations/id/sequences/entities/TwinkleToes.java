/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: TwinkleToes.java 14761 2008-06-11 13:51:06Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;
import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

/**
 * Blown precision on related entity when &#064;JoinColumn is used. 
 * Does not cause an issue on HyperSonic, but replicates nicely on PGSQL.
 * 
 * @see ANN-748
 * @author Andrew C. Oliver andyspam@osintegrators.com
 */
@Entity
@SuppressWarnings("serial")
public class TwinkleToes implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "java5_uuid")
	@GenericGenerator(name = "java5_uuid", strategy = "org.hibernate.test.annotations.id.UUIDGenerator")
	@Column(name = "id", precision = 128, scale = 0)
	private BigDecimal id;

	@ManyToOne
	@JoinColumn(name = "bunny_id")
	Bunny bunny;

	public void setBunny(Bunny bunny) {
		this.bunny = bunny;
	}

	public BigDecimal getId() {
		return id;
	}
}
