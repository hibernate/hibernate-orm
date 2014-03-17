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
package org.hibernate.metamodel.source.spi;

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
 * Not only does it provide access to the relational value sources ({@link #relationalValueSources()}, it also defines
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
	public List<RelationalValueSource> relationalValueSources();

}
