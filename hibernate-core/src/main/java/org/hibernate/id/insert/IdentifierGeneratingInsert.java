/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.Insert;

/**
 * Nothing more than a distinguishing subclass of Insert used to indicate
 * intent.  Some subclasses of this also provided some additional
 * functionality or semantic to the generated SQL statement string.
 *
 * @author Steve Ebersole
 */
public class IdentifierGeneratingInsert extends Insert {
	public IdentifierGeneratingInsert(Dialect dialect) {
		super( dialect );
	}
}
