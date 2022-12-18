/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.generatedkeys.selectannotated;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.id.SelectGenerator;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
@Entity @Table(name="my_entity")
@GenericGenerator(name = "triggered", type = SelectGenerator.class)
public class MyEntity {
	@Id @GeneratedValue(generator = "triggered")
	@ColumnDefault("-666") //workaround for h2 'before insert' triggers being crap
	private Long id;

	@NaturalId
	private String name;

	public MyEntity() {
	}

	public MyEntity(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
