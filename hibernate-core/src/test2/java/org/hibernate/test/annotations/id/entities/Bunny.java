/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.id.entities;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

/**
 * Blown precision on related entity when &#064;JoinColumn is used.
 * 
 * @see ANN-748
 * @author Andrew C. Oliver andyspam@osintegrators.com
 */
@Entity
@SuppressWarnings("serial")
public class Bunny implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "java5_uuid")
	@GenericGenerator(name = "java5_uuid", strategy = "org.hibernate.test.annotations.id.UUIDGenerator")
	@Column(name = "id", precision = 128, scale = 0)
	private BigDecimal id;

	@OneToMany(mappedBy = "bunny", cascade = { CascadeType.PERSIST })
	Set<PointyTooth> teeth;
	
	@OneToMany(mappedBy = "bunny", cascade = { CascadeType.PERSIST })
	Set<TwinkleToes> toes;

	public void setTeeth(Set<PointyTooth> teeth) {
		this.teeth = teeth;
	}

	public BigDecimal getId() {
		return id;
	}
}
