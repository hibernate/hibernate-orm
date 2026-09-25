package org.hibernate.sql.spi.mutation;

/**
 * The type of mutation
 *
 * @author Steve Ebersole
 */
public enum MutationType {
	INSERT( true ),
	UPDATE( true ),
	DELETE( false );

	private final boolean canSkipTables;

	MutationType(boolean canSkipTables) {
		this.canSkipTables = canSkipTables;
	}

	public boolean canSkipTables() {
		return canSkipTables;
	}
}
