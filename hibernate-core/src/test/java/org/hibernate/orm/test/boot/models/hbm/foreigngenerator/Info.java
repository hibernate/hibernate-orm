/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.foreigngenerator;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.ForeignGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Info {
	@Id
	@GeneratedValue( generator = "foreign" )
	@GenericGenerator(
			name = "foreign",
			type = ForeignGenerator.class,
			parameters = @Parameter( name = "property", value = "owner" )
	)
	private Integer id;
	@Basic
	private String name;

	@ManyToOne
	private Thing owner;

	protected Info() {
		// for Hibernate use
	}

	public Info(Thing owner, String name) {
		this.owner = owner;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Thing getOwner() {
		return owner;
	}

	public void setOwner(Thing owner) {
		this.owner = owner;
	}
}
