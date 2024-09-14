/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;

/**
 * @author Brett Meyer
 */
@Entity
@Table(name="TableA")
@SecondaryTables({@SecondaryTable(name = "TableB")})
public class EntityWithNestedEmbeddables {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	private EmbeddableA embedA;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public EmbeddableA getEmbedA() {
		return embedA;
	}

	public void setEmbedA(EmbeddableA embedA) {
		this.embedA = embedA;
	}
}
