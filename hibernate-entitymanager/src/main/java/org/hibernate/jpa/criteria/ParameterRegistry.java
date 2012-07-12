/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.criteria;
import javax.persistence.criteria.ParameterExpression;

/**
 * A registry for parameters.  In criteria queries, parameters must be actively seeked out as expressions and predicates
 * are added to the {@link org.hibernate.criterion.CriteriaQuery}; this contract allows the various subcomponents to
 * register any parameters they contain.
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistry {
	/**
	 * Registers the given parameter with this regitry.
	 *
	 * @param parameter The parameter to register.
	 */
	public void registerParameter(ParameterExpression<?> parameter);
}
