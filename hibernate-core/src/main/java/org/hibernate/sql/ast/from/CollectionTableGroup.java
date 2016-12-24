/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.from;


import java.util.List;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.DomainReferenceImplementor;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;

/**
 * A TableSpecificationGroup for a collection reference
 *
 * @author Steve Ebersole
 */
public class CollectionTableGroup extends AbstractTableGroup {
	private final CollectionPersister persister;

	public CollectionTableGroup(
			TableSpace tableSpace,
			String uid,
			String aliasBase,
			CollectionPersister persister,
			PropertyPath propertyPath) {
		super( tableSpace, uid, aliasBase, propertyPath );
		this.persister = persister;
	}

	public CollectionPersister getPersister() {
		return persister;
	}

	@Override
	public Selectable getSelectable() {
		throw new NotYetImplementedException( );
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public DomainReferenceImplementor getDomainReference() {
		// todo : element?
		return persister.getElementReference();
	}

	@Override
	public List<ColumnBinding> getColumnBindings() {
		throw new NotYetImplementedException(  );
	}
}
