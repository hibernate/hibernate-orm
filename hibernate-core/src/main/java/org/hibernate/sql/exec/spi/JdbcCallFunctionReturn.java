/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

/**
 * Models the function return when the JdbcOperationQueryCall represents a call to a database
 * function.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcCallFunctionReturn extends JdbcCallParameterRegistration {
}
