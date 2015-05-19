/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.internal;

import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;

/**
 * @deprecated Use {@link StandardSQLExceptionConverter} with {@link SQLStateConversionDelegate}
 * instead
 *
 * @author Steve Ebersole
 */
@Deprecated
public class SQLStateConverter extends StandardSQLExceptionConverter implements SQLExceptionConverter {
	public SQLStateConverter(final ViolatedConstraintNameExtracter extracter) {
		super();
		final ConversionContext conversionContext = new ConversionContext() {
			@Override
			public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
				return extracter;
			}
		};
		addDelegate( new SQLStateConversionDelegate( conversionContext ) );
	}
}
