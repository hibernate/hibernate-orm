/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.insert.Values;

/**
 * @author Christian Beikov
 */
public class ValuesTableReference extends DerivedTableReference {

	private final List<Values> valuesList;

	public ValuesTableReference(
			List<Values> valuesList,
			String identificationVariable,
			List<String> columnNames,
			SessionFactoryImplementor sessionFactory) {
		super( identificationVariable, columnNames, false, sessionFactory );
		this.valuesList = valuesList;
	}

	@Override
	public String getTableId() {
		return null;
	}

	public List<Values> getValuesList() {
		return valuesList;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitValuesTableReference( this );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return null;
	}

}
