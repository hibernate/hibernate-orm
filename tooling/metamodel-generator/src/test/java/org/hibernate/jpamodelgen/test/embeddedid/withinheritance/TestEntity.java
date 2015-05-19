/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddedid.withinheritance;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Immutable
@Table(name = "ENTITY")
public class TestEntity implements Serializable {
	@EmbeddedId
	private Ref ref;

	@Column(name = "NAME", insertable = false, updatable = false, unique = true)
	private String name;
}


