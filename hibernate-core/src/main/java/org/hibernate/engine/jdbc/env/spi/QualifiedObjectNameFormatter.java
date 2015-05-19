/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;

/**
 * @author Steve Ebersole
 */
public interface QualifiedObjectNameFormatter {
	public String format(QualifiedTableName qualifiedTableName, Dialect dialect);
	public String format(QualifiedSequenceName qualifiedSequenceName, Dialect dialect);
	public String format(QualifiedName qualifiedName, Dialect dialect);
}
