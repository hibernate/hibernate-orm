/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

/**
 * Common representation of both SecondaryTable and joined-tables
 * from inheritance.
 *
 * @author Steve Ebersole
 */
public interface MappedJoin {
	MappedTable getMappedTable();

	boolean isOptional();

	boolean isInverse();

	MappedForeignKey getJoinMapping();

	List<PersistentAttributeMapping> getPersistentAttributes();

	List<PersistentAttributeMapping> getDeclaredPersistentAttributes();

	ExecuteUpdateResultCheckStyle getUpdateResultCheckStyle();
}
