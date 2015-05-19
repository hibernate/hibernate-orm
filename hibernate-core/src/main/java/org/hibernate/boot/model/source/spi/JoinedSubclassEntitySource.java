/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JoinedSubclassEntitySource extends SubclassEntitySource, ForeignKeyContributingSource {
	/**
	 * The {@code PrimaryKeyJoinColumns} mapping for the joined-subclass.
	 *
	 * @return The {@code PrimaryKeyJoinColumnSource} lists defined on the joined subclass or {@code null} otherwise.
	 */
	public List<ColumnSource> getPrimaryKeyColumnSources();
}
