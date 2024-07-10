/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.spi;

import org.hibernate.Incubating;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * An implementation of {@link NavigablePath} with special handling for treated paths.
 *
 * @author Christian Beikov
 */
@Incubating
public class TreatedNavigablePath extends NavigablePath {

	public TreatedNavigablePath(NavigablePath parent, String entityTypeName) {
		this( parent, entityTypeName, null );
	}

	public TreatedNavigablePath(NavigablePath parent, String entityTypeName, @Nullable String alias) {
		super(
				parent,
				"#" + entityTypeName,
				alias,
				"treat(" + parent + " as " + entityTypeName + ")",
				1
		);
		assert !( parent instanceof TreatedNavigablePath );
	}

	/**
	 * @deprecated No longer used
	 */
	@Deprecated( since = "6.6", forRemoval = true )
	protected static String calculateTreatedFullPath(@Nullable NavigablePath parent, String localName, @Nullable String alias) {
		return alias == null
				? "treat(" + parent + " as " + localName + ")"
				: "treat(" + parent + " as " + localName + ")(" + alias + ")";
	}

	@Override
	public NavigablePath treatAs(String entityName) {
		return new TreatedNavigablePath( castNonNull( getRealParent() ), entityName );
	}

	@Override
	public NavigablePath treatAs(String entityName, String alias) {
		return new TreatedNavigablePath( castNonNull( getRealParent() ), entityName, alias );
	}

//	@Override
//	public int hashCode() {
//		return getFullPath().hashCode();
//	}
//
//	@Override
//	public boolean equals(Object other) {
//		if ( other == null ) {
//			return false;
//		}
//
//		if ( other == this ) {
//			return true;
//		}
//
//		if ( ! ( other instanceof NavigablePath ) ) {
//			return false;
//		}
//
//		return getFullPath().equals( ( (NavigablePath) other ).getFullPath() );
//	}
}
