/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
	List<ColumnSource> getPrimaryKeyColumnSources();
}
