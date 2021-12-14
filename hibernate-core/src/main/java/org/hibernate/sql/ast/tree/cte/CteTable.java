/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * Describes the table definition for the CTE - its name amd its columns
 *
 * @author Steve Ebersole
 */
public class CteTable {
	private final SessionFactoryImplementor sessionFactory;
	private final String cteName;
	private final List<CteColumn> cteColumns;

	public CteTable(String cteName, EntityMappingType entityDescriptor) {
		final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
		final List<CteColumn> columns = new ArrayList<>( numberOfColumns );
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(columnIndex, selection) -> columns.add(
						new CteColumn("cte_" + selection.getSelectionExpression(), selection.getJdbcMapping() )
				)
		);
		this.cteName = cteName;
		this.cteColumns = columns;
		this.sessionFactory = entityDescriptor.getEntityPersister().getFactory();
	}

	public CteTable(String cteName, List<CteColumn> cteColumns, SessionFactoryImplementor sessionFactory) {
		this.cteName = cteName;
		this.cteColumns = cteColumns;
		this.sessionFactory = sessionFactory;
	}

	public String getTableExpression() {
		return cteName;
	}

	public List<CteColumn> getCteColumns() {
		return cteColumns;
	}

	public CteTable withName(String name) {
		return new CteTable( name, cteColumns, sessionFactory );
	}

}
