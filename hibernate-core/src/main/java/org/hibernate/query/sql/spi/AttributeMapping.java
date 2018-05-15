/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.NativeQuery;

/**
 * Models the explicit mapping of an entity attribute
 *
 * @author Steve Ebersole
 */
public class AttributeMapping implements NativeQuery.ReturnProperty {
	private final String attributeName;
	private List<String> columnAliases;

	public AttributeMapping(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public List<String> getColumnAliases() {
		return columnAliases == null ? Collections.emptyList() : Collections.unmodifiableList( columnAliases );
	}
//
//
//	SqlSelectionGroup resolveSqlSelectionGroup(PersistentAttribute attribute, NodeResolutionContext context) {
//		if ( columnAliases != null ) {
//			assert !columnAliases.isEmpty();
//			final SqlSelectionGroupImpl sqlSelectionGroup = new SqlSelectionGroupImpl();
//			for ( String columnAlias : columnAliases ) {
//				sqlSelectionGroup.addSqlSelection( new SqlSelectionImpl( columnAlias ) );
//			}
//			return sqlSelectionGroup;
//		}
//		else {
//			return context.getSqlExpressionResolver().resolveSqlSelectionGroup( attribute );
//		}
//	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		if ( columnAliases == null ) {
			columnAliases = new ArrayList<>();
		}
		columnAliases.add( columnAlias );

		return this;
	}

}
