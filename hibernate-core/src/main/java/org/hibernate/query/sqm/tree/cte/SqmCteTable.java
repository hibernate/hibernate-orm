/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.sql.ast.tree.cte.CteColumn;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTable implements Serializable {
	private final String cteName;
	private final List<SqmCteTableColumn> columns;

	public SqmCteTable(String cteName, EntityMappingType entityDescriptor) {
		final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
		final List<SqmCteTableColumn> columns = new ArrayList<>( numberOfColumns );
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final String idName;
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			idName = ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName();
		}
		else {
			idName = "id";
		}
		columns.add( new SqmCteTableColumn( this, idName, identifierMapping ) );
		this.cteName = cteName;
		this.columns = columns;
	}

	public SqmCteTable(String cteName, List<SqmCteTableColumn> columns) {
		this.cteName = cteName;
		this.columns = columns;
	}

	public String getCteName() {
		return cteName;
	}

	public List<SqmCteTableColumn> getColumns() {
		return columns;
	}

	public void visitColumns(Consumer<SqmCteTableColumn> columnConsumer) {
		for ( int i = 0; i < columns.size(); i++ ) {
			columnConsumer.accept( columns.get( i ) );
		}
	}
}
