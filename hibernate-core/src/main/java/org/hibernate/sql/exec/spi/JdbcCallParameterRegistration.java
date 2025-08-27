/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.sql.CallableStatement;
import jakarta.persistence.ParameterMode;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.OutputableType;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;

/**
 * @author Steve Ebersole
 */
public interface JdbcCallParameterRegistration {

	String getName();

	ParameterMode getParameterMode();

	void registerParameter(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session);

	JdbcParameterBinder getParameterBinder();

	JdbcCallParameterExtractor<?> getParameterExtractor();

	JdbcCallRefCursorExtractorImpl getRefCursorExtractor();

	OutputableType<?> getParameterType();
}
