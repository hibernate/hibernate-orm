/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.annotations;

import java.lang.annotation.Annotation;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.JpaCallbackSource;

/**
 * @author Hardy Ferentschik
 */
public class JpaCallbackSourceImpl implements JpaCallbackSource {
	private final JavaTypeDescriptor callbackTarget;
	private final boolean isEntityListener;
	private final String prePersistCallback;
	private final String preRemoveCallback;
	private final String preUpdateCallback;
	private final String postLoadCallback;
	private final String postPersistCallback;
	private final String postRemoveCallback;
	private final String postUpdateCallback;

	public JpaCallbackSourceImpl(
			JavaTypeDescriptor callbackTarget,
			boolean isEntityListener,
			String prePersistCallback,
			String preRemoveCallback,
			String preUpdateCallback,
			String postLoadCallback,
			String postPersistCallback,
			String postRemoveCallback,
			String postUpdateCallback) {
		this.callbackTarget = callbackTarget;
		this.isEntityListener = isEntityListener;
		this.prePersistCallback = prePersistCallback;
		this.preRemoveCallback = preRemoveCallback;
		this.preUpdateCallback = preUpdateCallback;
		this.postLoadCallback = postLoadCallback;
		this.postPersistCallback = postPersistCallback;
		this.postRemoveCallback = postRemoveCallback;
		this.postUpdateCallback = postUpdateCallback;
	}

	@Override
	public String getName() {
		return callbackTarget.getName().toString();
	}

	@Override
	public boolean isListener() {
		return isEntityListener;
	}

	@Override
	public String getCallbackMethod(Class<? extends Annotation> callbackType) {
		if ( PrePersist.class.equals( callbackType ) ) {
			return prePersistCallback;
		}
		else if ( PreRemove.class.equals( callbackType ) ) {
			return preRemoveCallback;
		}
		else if ( PreUpdate.class.equals( callbackType ) ) {
			return preUpdateCallback;
		}
		else if ( PostLoad.class.equals( callbackType ) ) {
			return postLoadCallback;
		}
		else if ( PostPersist.class.equals( callbackType ) ) {
			return postPersistCallback;
		}
		else if ( PostRemove.class.equals( callbackType ) ) {
			return postRemoveCallback;
		}
		else if ( PostUpdate.class.equals( callbackType ) ) {
			return postUpdateCallback;
		}

		throw new IllegalArgumentException( "Unknown callback type requested : " + callbackType.getName() );
	}

	@Override
	public String toString() {
		return "JpaCallbackSourceImpl{callbackTarget=" + callbackTarget +
				", isEntityListener=" + isEntityListener + '}';
	}
}


