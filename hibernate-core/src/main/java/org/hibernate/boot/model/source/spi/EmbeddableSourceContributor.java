/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract for things that can contain EmbeddableSource definitions.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableSourceContributor {
	/**
	 * Gets the source information about the embeddable/composition.
	 *
	 * @return The EmbeddableSource
	 */
	public EmbeddableSource getEmbeddableSource();
}
