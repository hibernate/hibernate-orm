/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.common.internal.DatabaseModel;
import org.hibernate.persister.common.internal.DomainMetamodelImpl;
import org.hibernate.persister.common.spi.SqmTypeImplementor;
import org.hibernate.sql.sqm.ast.from.CollectionTableGroup;
import org.hibernate.sql.sqm.ast.from.TableSpace;
import org.hibernate.sql.sqm.convert.internal.FromClauseIndex;
import org.hibernate.sql.sqm.convert.internal.SqlAliasBaseManager;
import org.hibernate.sqm.domain.PluralAttributeReference;
import org.hibernate.sqm.query.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public interface ImprovedCollectionPersister extends PluralAttributeReference, SqmTypeImplementor {
	CollectionPersister getPersister();

	void finishInitialization(DatabaseModel databaseModel, DomainMetamodelImpl domainMetamodel);

	PluralAttributeKey getForeignKeyDescriptor();
	PluralAttributeId getIdDescriptor();
	PluralAttributeIndex getIndexDescriptor();
	PluralAttributeElement getElementDescriptor();

	CollectionTableGroup buildTableGroup(
			SqmAttributeJoin joinedFromElement,
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex);
}
