/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

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
