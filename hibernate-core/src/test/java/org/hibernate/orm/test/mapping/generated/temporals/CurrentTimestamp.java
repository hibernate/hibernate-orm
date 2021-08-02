/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.tuple.GenerationTiming;

/**
 * @author Steve Ebersole
 */
@ValueGenerationType(generatedBy = CurrentTimestampGeneration.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentTimestamp {
	GenerationTiming timing();
}
