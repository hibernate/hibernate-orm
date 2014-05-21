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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.metamodel.source.internal.jaxb.hbm.HbmXmlTransformer;

/**
 * Can be used to mark an association as the inverse side, without explicitly identifying the "mappedBy" on the
 * association annotation itself.  This solely exists for transforming HBM to JPA (see {@link HbmXmlTransformer}).
 * Direct use should be completely avoided.
 *
 * @author Brett Meyer
 * 
 * @deprecated Use {@link OneToOne#mappedBy()}, {@link OneToMany#mappedBy()}, or {@link ManyToMany#mappedBy()}.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated
public @interface Inverse {
	
	/**
	 * Primarily used to carry a M2M inverse side's <key>.
	 * 
	 * @return hbmKey
	 */
	String hbmKey();
}
