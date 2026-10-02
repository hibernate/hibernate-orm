package org.hibernate.persister.state.internal;

/**
 * @author Gavin King
 *
 * @since 7.4
 */
public final class StandardStateManagement extends AbstractStateManagement {
	public static final StandardStateManagement INSTANCE = new StandardStateManagement();

	private StandardStateManagement() {
	}
}
