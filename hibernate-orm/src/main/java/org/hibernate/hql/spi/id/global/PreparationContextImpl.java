/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.global;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl;

/**
 * PreparationContext implementation for GlobalTemporaryTableBulkIdStrategy.  Used to collect
 * create and drop statements
 *
 * @author Steve Ebersole
 */
class PreparationContextImpl implements AbstractMultiTableBulkIdStrategyImpl.PreparationContext {
	List<String> creationStatements = new ArrayList<String>();
	List<String> dropStatements = new ArrayList<String>();
}
