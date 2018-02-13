/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */


package org.hibernate.cfg.annotations.schema.entities.global;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;


/**
 * Exemple of global schema data.
 * @author Benoit Besson
 */
@Entity
@Table(name = "t_global")
public class Global {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_global")
	@SequenceGenerator(name = "seq_global", sequenceName = "seq_global")
	private Long id;
	
	private String name;

	public Global() {
	}

	public Global(Long id) {
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
		return "Global: " + id + " " + name;
	}
}
