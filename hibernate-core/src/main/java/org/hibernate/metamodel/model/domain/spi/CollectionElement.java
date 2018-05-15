/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.CollectionDomainType;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;

/**
 * @author Steve Ebersole
 */
public interface CollectionElement<J> extends Navigable<J>, CollectionDomainType.Element<J>, TableReferenceContributor {
	String NAVIGABLE_NAME = "{element}";

	boolean canContainSubGraphs();

	enum ElementClassification {
		BASIC,
		EMBEDDABLE,
		ANY,
		ONE_TO_MANY,
		MANY_TO_MANY;
	}

	ElementClassification getClassification();

	PersistentCollectionDescriptor getCollectionDescriptor();

	Table getPrimaryDmlTable();

	SimpleTypeDescriptor getDomainTypeDescriptor();

	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
