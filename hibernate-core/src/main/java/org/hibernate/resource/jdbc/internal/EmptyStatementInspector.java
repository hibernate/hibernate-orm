package org.hibernate.resource.jdbc.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * @author Jan Schatteman
 */
public class EmptyStatementInspector implements StatementInspector {
	/**
	 * The singleton reference.
	 */
	public static final StatementInspector INSTANCE = new EmptyStatementInspector();

	@Override
	@Nonnull
	public String inspect(@Nonnull String sql) {
		return sql;
	}

	protected EmptyStatementInspector() {}
}
