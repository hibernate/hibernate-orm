/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.StringHelper;

/**
 * Revision number generator has to produce values in ascending order (gaps may occur).
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OrderedSequenceGenerator extends SequenceStyleGenerator {
	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		String[] create = super.sqlCreateStrings( dialect );
		if ( dialect instanceof Oracle8iDialect ) {
			// Make sure that sequence produces increasing values in Oracle RAC environment.
			create = StringHelper.suffix( create, " order" );
		}
		return create;
	}
}
