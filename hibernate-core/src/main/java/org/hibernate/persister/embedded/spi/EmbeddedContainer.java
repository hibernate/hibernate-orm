/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.spi;

import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.queryable.spi.NavigableReferenceInfo;
import org.hibernate.persister.queryable.spi.TableGroupResolver;
import org.hibernate.sql.tree.from.TableGroup;

/**
 * Contract for things that can contain composites.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedContainer<T> extends NavigableSource<T> {
	/**
	 * Resolve the TableGroup that defines the group of tables which
	 * contain the composite values.
	 *
	 * @param embeddedReferenceInfo Information about the embedded-valued navigable reference
	 * @param tableGroupResolver Access to existing TableGroup resolutions
	 *
	 * @return The resolved TableGroup
	 */
	TableGroup resolveTableGroup(NavigableReferenceInfo embeddedReferenceInfo, TableGroupResolver tableGroupResolver);

	/**
	 * Holy alliteration Bat Man! :)
	 * <p/>
	 * Can any sub-composites belonging to this container contain collections?  This
	 * would only be {@code false} in cases where the composite-container path
	 * is itself rooted in a collection element/index.
	 *
	 * @return {@code true} if the composites can contain collections; {@code false}
	 * otherwise.
	 */
	boolean canCompositeContainCollections();

	String getRolePrefix();
}
