/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
