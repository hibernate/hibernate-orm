/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public interface PostActionEventListener extends Serializable {
	/**
	 * Does this listener require that after transaction hooks be registered?
	 *
	 * @param descriptor The descriptor for the entity in question.
	 *
	 * @return {@code true} if after transaction callbacks should be added.
	 */
	boolean requiresPostCommitHandling(EntityTypeDescriptor descriptor);
}

