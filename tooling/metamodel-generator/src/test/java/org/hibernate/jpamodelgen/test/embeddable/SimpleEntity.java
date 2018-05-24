/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class SimpleEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@NotNullAllowed
	@Embedded
	private SimpleEmbeddable simpleEmbeddable;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public SimpleEmbeddable getSimpleEmbeddable() {
		return simpleEmbeddable;
	}

	public void setSimpleEmbeddable(SimpleEmbeddable simpleEmbeddable) {
		this.simpleEmbeddable = simpleEmbeddable;
	}

	// represents a mock TYPE_USE based annotation
	@Target({ ElementType.TYPE_USE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NotNullAllowed {
	}
}
