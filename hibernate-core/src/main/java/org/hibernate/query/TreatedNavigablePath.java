/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * @author Christian Beikov
 */
public class TreatedNavigablePath extends NavigablePath {

	public static final String ROLE_LOCAL_NAME = "{treated}";

	public TreatedNavigablePath(NavigablePath parent, String entityTypeName) {
		super( parent, ROLE_LOCAL_NAME, entityTypeName );
	}

	@Override
	public String getLocalName() {
		return ROLE_LOCAL_NAME;
	}

	@Override
	public int hashCode() {
		return getParent().getFullPath().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}

		if ( other == this ) {
			return true;
		}

		if ( ! ( other instanceof NavigablePath ) ) {
			return false;
		}

		return getFullPath().equals( ( (NavigablePath) other ).getFullPath() );
	}
}
