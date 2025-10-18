/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

/**
 * Minimal lexer interface that allows HqlAnalyzer to work.
 *
 * @author Max Rydahl Andersen
 */
public interface SimpleHQLLexer {


	int nextTokenId() throws SimpleLexerException;

	int getTokenOffset();

	int getTokenLength();

}
