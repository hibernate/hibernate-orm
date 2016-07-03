/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.ModificationStore;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.Type;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public class RevisionTimestampData extends PropertyData {

	private Type type;

	public RevisionTimestampData(String name, String beanName, String accessType, ModificationStore store, Type type) {
		super( name, beanName, accessType, store );
		this.type = type;
	}

	public RevisionTimestampData(RevisionTimestampData old, Type type) {
		this( old.getName(), old.getBeanName(), old.getAccessType(), old.getStore(), type );
	}

	public Type getType() {
		return type;
	}

	public boolean isTimestampDate() {
		if ( type != null ) {
			final String typename = type.getName();
			return "date".equals( typename )
					|| "time".equals( typename )
					|| "timestamp".equals( typename );
		}
		return false;
	}

	public boolean isTimestampLocalDateTime() {
		if ( type != null ) {
			final String typename = type.getName();
			return "LocalDateTime".equals( typename );
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ( type != null ? type.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final RevisionTimestampData that = (RevisionTimestampData) o;
		return super.equals( o ) && EqualsHelper.equals( type, that.type );
	}
}
