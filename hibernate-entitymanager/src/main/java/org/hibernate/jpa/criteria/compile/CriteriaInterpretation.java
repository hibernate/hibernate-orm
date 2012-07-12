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
package org.hibernate.jpa.criteria.compile;

import javax.persistence.Query;

import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

/**
 * The interpretation of a JPA criteria object.
 *
 * @author Steve Ebersole
 */
public interface CriteriaInterpretation {
	/**
	 * Generate a {@link javax.persistence.Query} instance given the interpreted criteria compiled against the
	 * passed EntityManager.
	 *
	 *
	 * @param entityManager The EntityManager against which to create the Query instance.
	 * @param interpretedParameterMetadata parameter metadata
	 *
	 * @return The created Query instance.
	 */
	public Query buildCompiledQuery(HibernateEntityManagerImplementor entityManager, InterpretedParameterMetadata interpretedParameterMetadata);
}
