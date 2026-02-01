/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;

/**
 * Metadata about audit log tables for entities and collections enabled for audit logging.
 *
 * @see org.hibernate.annotations.Audited
 */
public interface AuditMapping {
	String getTableName();

	String getTransactionIdColumnName();

	String getModificationTypeColumnName();

	SelectableMapping getTransactionIdMapping();

	SelectableMapping getModificationTypeMapping();

	JdbcMapping getJdbcMapping();

	Predicate createRestriction(
			TableGroupProducer tableGroupProducer,
			TableReference tableReference,
			List<SelectableMapping> keySelectables,
			SqlAliasBaseGenerator sqlAliasBaseGenerator);
//
//	ColumnValueBinding createTransactionIdValueBinding(ColumnReference columnReference);
//
//	ColumnValueBinding createModificationTypeValueBinding(ColumnReference columnReference, int modificationType);
}
