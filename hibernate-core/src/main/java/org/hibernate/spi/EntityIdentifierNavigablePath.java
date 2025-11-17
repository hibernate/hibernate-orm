/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;

/**
 * Specialized implementation of {@link NavigablePath} for handling special cases
 * pertaining to entity identifiers.
 *
 * @author Andrea Boriero
 */
@Incubating
public class EntityIdentifierNavigablePath extends NavigablePath {
	private final String identifierAttributeName;

	public EntityIdentifierNavigablePath(NavigablePath parent, String identifierAttributeName) {
		super( parent, EntityIdentifierMapping.ID_ROLE_NAME );
		this.identifierAttributeName = identifierAttributeName;
	}

	public EntityIdentifierNavigablePath(NavigablePath parent, String alias, String identifierAttributeName) {
		super( parent, EntityIdentifierMapping.ID_ROLE_NAME, alias );
		this.identifierAttributeName = identifierAttributeName;
	}

	public String getIdentifierAttributeName() {
		return identifierAttributeName;
	}

	@Override
	public String getLocalName() {
		return EntityIdentifierMapping.ID_ROLE_NAME;
	}

	@Override
	protected boolean localNamesMatch(DotIdentifierSequence otherPath) {
		final String otherLocalName = otherPath.getLocalName();

		return otherLocalName.equals( getLocalName() )
				|| otherLocalName.equals( identifierAttributeName );
	}

	@Override
	protected boolean localNamesMatch(EntityIdentifierNavigablePath otherPath) {
		return super.localNamesMatch( otherPath );
	}

//	@Override
//	public int hashCode() {
//		return getParent().getFullPath().hashCode();
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
//		final NavigablePath otherPath = (NavigablePath) other;
//
//		if ( getFullPath().equals( ( (NavigablePath) other ).getFullPath() ) ) {
//			return true;
//		}
//
//		if ( getParent() == null ) {
//			if ( otherPath.getParent() != null ) {
//				return false;
//			}
//
//			//noinspection RedundantIfStatement
//			if ( localNamesMatch(  otherPath) ) {
//				return true;
//
//			}
//
//			return false;
//		}
//
//		if ( otherPath.getParent() == null ) {
//			return false;
//		}
//
//		return getParent().equals( otherPath.getParent() )
//				&& localNamesMatch( otherPath );
//	}
//
//	private boolean localNamesMatch(NavigablePath otherPath) {
//		final String otherLocalName = otherPath.getLocalName();
//
//		return otherLocalName.equals( getLocalName() )
//				|| otherLocalName.equals( identifierAttributeName );
//	}
}
