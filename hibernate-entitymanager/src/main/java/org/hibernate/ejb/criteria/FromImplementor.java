/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria;

import java.io.Serializable;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;

/**
 * Implementation contract for the JPA {@link From} interface.
 *
 * @author Steve Ebersole
 */
public interface FromImplementor<Z,X> extends PathImplementor<X>, From<Z,X> {
	public void prepareAlias(CriteriaQueryCompiler.RenderingContext renderingContext);
	public String renderTableExpression(CriteriaQueryCompiler.RenderingContext renderingContext);


	public FromImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);
	public void prepareCorrelationDelegate(FromImplementor<Z,X> parent);
	public FromImplementor<Z, X> getCorrelationParent();
}
