/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Apply a cascade strategy on an association.  Used to apply Hibernate specific cascades.  For JPA cascading, prefer
 * using {@link javax.persistence.CascadeType} on {@link javax.persistence.OneToOne},
 * {@link javax.persistence.OneToMany}, etc.  Hibernate will merge together both sets of cascades.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cascade {
	/**
	 * The cascade value.
	 */
	CascadeType[] value();
}
