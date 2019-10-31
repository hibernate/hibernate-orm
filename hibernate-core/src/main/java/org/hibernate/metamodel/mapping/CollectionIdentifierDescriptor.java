/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * Descriptor for the collection identifier.  Only used with {@link CollectionClassification#IDBAG} collections
 *
 * @author Steve Ebersole
 */
public interface CollectionIdentifierDescriptor {
	DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState);
}
