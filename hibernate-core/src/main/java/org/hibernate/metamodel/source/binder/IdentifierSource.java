/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.binder;

import org.hibernate.metamodel.binding.IdGenerator;

/**
 * Contract describing source of identifier information for the entity.
 *
 * @author Steve Ebersole
 */
public interface IdentifierSource {
    /**
     * Obtain the identifier generator source.
     *
     * @return The generator source.
     */
    IdGenerator getIdentifierGeneratorDescriptor();

    public static enum Nature {
		/**
		 * A single, simple identifier.  Equivalent of an {@code <id/>} mapping or a single {@code @Id}
		 * annotation.  Indicates the {@link IdentifierSource} is castable to {@link SimpleIdentifierSource}.
		 */
		SIMPLE,

		/**
		 * What we used to term an "embedded composite identifier", which is not to be confused with the JPA
		 * term embedded.  Specifically a composite id where there is no component class, though there may be an
		 * {@code @IdClass}.
		 */
		COMPOSITE,

		/**
		 * Composite identifier with an actual component class used to aggregate the individual attributes
		 */
		AGGREGATED_COMPOSITE
	}

	/**
	 * Obtain the nature of this identifier source.
	 *
	 * @return The identifier source's nature.
	 */
	public Nature getNature();
}
