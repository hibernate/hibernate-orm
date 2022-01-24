/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

/**
 * @author Christian Beikov
 */
public class TreatedNavigablePath extends NavigablePath {

	public TreatedNavigablePath(NavigablePath parent, String entityTypeName) {
		this( parent, entityTypeName, null );
	}

	public TreatedNavigablePath(NavigablePath parent, String entityTypeName, String alias) {
		super(
				parent,
				alias == null ? "treat(" + parent.getFullPath() + " as " + entityTypeName + ")"
						: "treat(" + parent.getFullPath() + " as " + entityTypeName + ")(" + alias + ")",
				entityTypeName,
				"treat(" + parent.getFullPath() + " as " + entityTypeName + ")"
		);
		assert !( parent instanceof TreatedNavigablePath );
	}

	@Override
	public NavigablePath treatAs(String entityName) {
		return new TreatedNavigablePath( getRealParent(), entityName );
	}

	@Override
	public NavigablePath treatAs(String entityName, String alias) {
		return new TreatedNavigablePath( getRealParent(), entityName, alias );
	}

	@Override
	public String getLocalName() {
		return getUnaliasedLocalName();
	}

	@Override
	public int hashCode() {
		return getFullPath().hashCode();
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
