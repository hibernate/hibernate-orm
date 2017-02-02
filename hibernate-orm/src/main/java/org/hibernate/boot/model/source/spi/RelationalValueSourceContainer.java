/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * Contract for a container of {@link RelationalValueSource} references.  Multiple types of things operate as sources
 * of "relational value" information; some examples include:<ul>
 *     <li>id attribute(s) mappings</li>
 *     <li>basic attribute mappings</li>
 *     <li>composite attribute mappings</li>
 *     <li>plural attribute mappings</li>
 *     <li>etc</li>
 * </ul>
 *
 * Not only does it provide access to the relational value sources ({@link #getRelationalValueSources()}, it also defines
 * contextual information for those sources in terms of default values.
 *
 * See {@link RelationalValueSource} for additional details.
 *
 * @author Steve Ebersole
 */
public interface RelationalValueSourceContainer extends ColumnBindingDefaults {
	/**
	 * Obtain the contained {@link RelationalValueSource} references.
	 *
	 * @return The contained {@link RelationalValueSource} references.
	 */
	public List<RelationalValueSource> getRelationalValueSources();

}
