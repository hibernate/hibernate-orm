/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedSequenceName;

/**
 * @author Steve Ebersole
 */
public class Sequence implements Exportable {
	private int initialValue = 1;
	private int incrementSize = 1;
	private final Identifier name;
	private final QualifiedSequenceName qualifiedName;

	public Sequence(
			Identifier logicalCatalogName,
			Identifier logicalSchemaName,
			Identifier logicalSequenceName,
			int initialValue,
			int incrementSize,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		this.name = namingStrategy.toPhysicalTableName( logicalSequenceName, jdbcEnvironment );
		this.qualifiedName = new QualifiedSequenceName(
				namingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
				namingStrategy.toPhysicalCatalogName( logicalSchemaName, jdbcEnvironment ),
				name
		);

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getIncrementSize() {
		return incrementSize;
	}

	public Identifier getName() {
		return name;
	}

	public QualifiedSequenceName getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public String getExportIdentifier() {
		return getName().render();
	}

	@Override
	public String toString() {
		return "Sequence(" + getName() + ")";
	}
}
