/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * Contract for contextual information required to perform translation.
*
* @author Steve Ebersole
*/
public interface TranslationContext extends ParameterBindingContext {
}
