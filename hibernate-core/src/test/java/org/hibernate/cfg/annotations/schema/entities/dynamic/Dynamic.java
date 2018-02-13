/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */


package org.hibernate.cfg.annotations.schema.entities.dynamic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import org.hibernate.cfg.annotations.schema.entities.global.Global;

/**
 * Exemple of dynamic schema data.
 * @author Benoit Besson
 */
@Entity
@Table(name = "t_dynamic")
public class Dynamic {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_dynamic")
	@SequenceGenerator(name = "seq_dynamic", sequenceName = "seq_dynamic")
	private Long id;
	
	String name;

	@ManyToOne
	Global global;

	public Dynamic() {
	}

	public Dynamic(Long id) {
		this.id = id;
	}

	public String Name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Dynamic: " + id + " " + name;
	}
}
