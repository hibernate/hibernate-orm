/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * Additional contract describing the source of an identifier mapping whose
 * {@link #getNature() nature} is
 * {@link org.hibernate.id.EntityIdentifierNature#AGGREGATED_COMPOSITE}.
 * <p/>
 * This equates to an identifier which is made up of multiple values which are
 * defined as part of a component/embedded; i.e. {@link javax.persistence.EmbeddedId}
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface IdentifierSourceAggregatedComposite extends CompositeIdentifierSource {
	/**
	 * Obtain the source descriptor for the identifier attribute.
	 *
	 * @return The identifier attribute source.
	 */
	public SingularAttributeSourceEmbedded getIdentifierAttributeSource();

	/**
	 * Obtain the mapping of attributes annotated with {@link javax.persistence.MapsId}.
	 *
	 * @return The MapsId sources.
	 */
	public List<MapsIdSource> getMapsIdSources();
}
