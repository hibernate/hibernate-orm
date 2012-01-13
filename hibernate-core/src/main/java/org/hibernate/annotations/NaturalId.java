/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.NaturalIdMutability;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This specifies that a property is part of the natural id of the entity.
 *
 * @author Nicol�s Lichtmaier
 * @author Steve Ebersole
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface NaturalId {
	/**
	 * @deprecated Use {@link #mutability()} instead.  For {@code mutable == false} (the default) use
	 * {@link NaturalIdMutability#IMMUTABLE_CHECKED}; for {@code mutable == true} use
	 * {@link NaturalIdMutability#MUTABLE}.
	 *
	 * Note however the difference between {@link NaturalIdMutability#IMMUTABLE_CHECKED} which mimics the old behavior
	 * of {@code mutable == false} and the new behavior available via {@link NaturalIdMutability#IMMUTABLE}
	 */
	@Deprecated
	@SuppressWarnings( {"JavaDoc"})
	boolean mutable() default false;

	/**
	 * The mutability behavior of this natural id
	 *
	 * @return The mutability behavior.
	 */
	NaturalIdMutability mutability();
}
