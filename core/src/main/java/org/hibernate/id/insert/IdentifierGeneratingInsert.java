package org.hibernate.id.insert;

import org.hibernate.sql.Insert;
import org.hibernate.dialect.Dialect;

/**
 * Nothing more than a distinguishing subclass of Insert used to indicate
 * intent.  Some subclasses of this also provided some additional
 * functionality or semantic to the genernated SQL statement string.
 *
 * @author Steve Ebersole
 */
public class IdentifierGeneratingInsert extends Insert {
	public IdentifierGeneratingInsert(Dialect dialect) {
		super( dialect );
	}
}
