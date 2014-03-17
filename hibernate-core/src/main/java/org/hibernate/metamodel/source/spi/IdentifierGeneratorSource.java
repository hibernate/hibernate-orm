/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface IdentifierGeneratorSource {
	/**
	 * Retrieve the name of the generator specification.  This is the name used in
	 * {@link javax.persistence.GeneratedValue}, not the name of the underlying table/sequence!
	 *
	 * @return The generator name
	 */
	public String getGeneratorName();

	/**
	 * Retrieve the name of the generator implementation name.  This is either<ul>
	 *     <li>an FQN naming the {@link org.hibernate.id.IdentifierGenerator} implementation</li>
	 *     <li>the recognized "short name" of a built-in {@link org.hibernate.id.IdentifierGenerator} implementation</li>
	 * </ul>
	 *
	 * @return the generator implementation name
	 */
	public String getGeneratorImplementationName();

	/**
	 * Retrieve the generator config parameters
	 *
	 * @return generator configuration parameters
	 */
	public Map<String, String> getParameters();
}
