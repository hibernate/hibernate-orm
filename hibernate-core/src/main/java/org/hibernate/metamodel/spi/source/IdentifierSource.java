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
package org.hibernate.metamodel.spi.source;

import org.hibernate.metamodel.spi.binding.IdGenerator;

/**
 * Contract describing source of identifier information for the entity.
 *
 * @author Steve Ebersole
 */
public interface IdentifierSource {
    /**
     * Obtain the identifier generator source.
	 *
	 * @todo this should name a source as well, no?
	 * 		Basically, not sure it should be up to the sources to build binding objects.
	 * 		IdentifierGeneratorSource, possibly as a hierarchy as well to account for differences
	 * 		in "global" versus "local" declarations
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
		 * term embedded.  Specifically a composite id where there is no component class (though there may be an
		 * {@code @IdClass}).  Indicates that the {@link IdentifierSource} is castable to
		 * {@link NonAggregatedCompositeIdentifierSource}
		 */
		COMPOSITE,

		/**
		 * Composite identifier with an actual component class used to aggregate the individual attributes.  Indicates
		 * that the {@link IdentifierSource} is castable to {@link AggregatedCompositeIdentifierSource}
		 */
		AGGREGATED_COMPOSITE
	}

	/**
	 * Obtain the nature of this identifier source.
	 *
	 * @return The identifier source's nature.
	 */
	public Nature getNature();

	/**
	 *  Returns the "unsaved" entity identifier value.
	 *
	 *  @todo Not sure this is relevant for anything other than simple identifiers.  Move to SimpleIdentifierSource ?
	 *
	 *  @return the "unsaved" entity identifier value
	 */
	public String getUnsavedValue();
}
