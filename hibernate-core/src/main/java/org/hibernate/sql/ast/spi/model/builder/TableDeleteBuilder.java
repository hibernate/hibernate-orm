/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.model.TableDelete;
import org.hibernate.sql.spi.mutation.jdbc.JdbcDeleteMutation;

import static org.hibernate.SPI.Role.IMPLEMENT;

/**
 * {@link TableMutationBuilder} implementation for {@code delete} statements.
 *
 * @author Steve Ebersole
 */
@SPI( IMPLEMENT )
public interface TableDeleteBuilder extends RestrictedTableMutationBuilder<JdbcDeleteMutation, TableDelete> {

}
