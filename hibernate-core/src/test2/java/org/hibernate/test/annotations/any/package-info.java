/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
@AnyMetaDef( name= "Property", metaType = "string", idType = "integer",
	metaValues = {
			@MetaValue(value = "C", targetEntity = CharProperty.class),
			@MetaValue(value = "I", targetEntity = IntegerProperty.class),
			@MetaValue(value = "S", targetEntity = StringProperty.class),
			@MetaValue(value = "L", targetEntity = LongProperty.class)
			})

package org.hibernate.test.annotations.any;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.MetaValue;

