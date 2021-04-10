/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import static org.hibernate.internal.util.Validator.checkNotNullIAE;

import javax.persistence.NamedEntityGraph;

import org.hibernate.internal.util.StringHelper;

/**
 * Models the definition of a {@link NamedEntityGraph} annotation
 *
 * @author Steve Ebersole
 */
public class NamedEntityGraphDefinition {
	private final NamedEntityGraph annotation;
	private final String jpaEntityName;
	private final String entityName;
	private final String name;

	public NamedEntityGraphDefinition(NamedEntityGraph annotation, String jpaEntityName, String entityName) {
		this.annotation = annotation;
		this.jpaEntityName = jpaEntityName;
		this.entityName = entityName;
		this.name = StringHelper.isNotEmpty( annotation.name() )
				? annotation.name()
				: jpaEntityName;
		checkNotNullIAE( "name", name );
	}

	public String getRegisteredName() {
		return name;
	}

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public NamedEntityGraph getAnnotation() {
		return annotation;
	}
}
