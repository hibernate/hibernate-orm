/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardAuxiliaryDatabaseObjectExporter implements Exporter<AuxiliaryDatabaseObject> {
	private final Dialect dialect;

	public StandardAuxiliaryDatabaseObjectExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(AuxiliaryDatabaseObject object,RuntimeModelCreationContext modelCreationContext) {
		return object.sqlCreateStrings( dialect );
	}

	@Override
	public String[] getSqlDropStrings(AuxiliaryDatabaseObject object, RuntimeModelCreationContext modelCreationContext) {
		return object.sqlDropStrings( dialect );
	}
}
