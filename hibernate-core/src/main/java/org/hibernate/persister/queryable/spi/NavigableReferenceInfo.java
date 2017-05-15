/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public interface NavigableReferenceInfo extends TableGroupInfoSource, ExpressableType {
	NavigableContainerReferenceInfo getNavigableContainerReferenceInfo();

	Navigable getReferencedNavigable();

	NavigablePath getNavigablePath();
}
