/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "the_entity")
@EntityListeners( TheListener.class )
public class TheEntity {
	private Integer id;
	private String name;

	public TheEntity() {
	}

	public TheEntity(Integer id) {
		this.id = id;
	}

	@Id
    @GeneratedValue(generator = "mygenerator")
    @GenericGenerator(name = "mygenerator", strategy = "org.hibernate.test.cdi.events.MyIdGenerator")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
