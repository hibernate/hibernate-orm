/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.annotations.GenericGenerator;

/**
 * The {@link Simple} entity redone with a generated value {@link #id1} }as part of its
 * composite pk
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
@Entity
@IdClass(SimplePK.class)
@SuppressWarnings("serial")
public class Simple2 implements Serializable {
	@Id
	@GenericGenerator(name = "increment", strategy = "increment")
	@GeneratedValue(generator = "increment")
	private Long id1;

	@Id
	private Long id2;

	private int quantity;

	public Simple2() {
	}

	public Simple2(Long id, int quantity) {
		this.id2 = id;
		this.quantity = quantity;
	}

	public Long getId1() {
		return id1;
	}

	public Long getId2() {
		return id2;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
