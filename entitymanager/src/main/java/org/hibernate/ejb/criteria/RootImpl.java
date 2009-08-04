/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;

/**
 * Defines a <tt>query root</tt>.
 *
 * @author Steve Ebersole
 */
public class RootImpl<X> extends FromImpl<X,X> implements Root<X> {
	public RootImpl(
			QueryBuilderImpl queryBuilder,
			EntityType<X> model) {
		super( queryBuilder, model );
	}

	@Override
    public EntityType<X> getModel(){
        return ( EntityType<X> ) super.getModel();
    }

	@Override
	protected Attribute<X, ?> getAttribute(String name) {
		return (Attribute<X, ?>) getModel().getAttribute( name );
	}

}
