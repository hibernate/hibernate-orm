/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import jakarta.persistence.TemporalType;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class InstantType
		extends AbstractSingleColumnStandardBasicType<Instant>
		implements VersionType<Instant>, AllowableTemporalParameterType<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantType INSTANCE = new InstantType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S 'Z'", Locale.ENGLISH );

	public InstantType() {
		super( TimestampTypeDescriptor.INSTANCE, InstantJavaDescriptor.INSTANCE );
	}

	@Override
	public Instant seed(SharedSessionContractImplementor session) {
		return Instant.now();
	}

	@Override
	public Instant next(Instant current, SharedSessionContractImplementor session) {
		return Instant.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<Instant> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return "instant";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public AllowableTemporalParameterType resolveTemporalPrecision(
			TemporalType temporalPrecision,
			TypeConfiguration typeConfiguration) {
		switch ( temporalPrecision ) {
			case TIMESTAMP: {
				return this;
			}
			case TIME: {
				return TimeType.INSTANCE;
			}
			case DATE: {
				return DateType.INSTANCE;
			}
		}
		throw new QueryException( "Instant type cannot be treated using `" + temporalPrecision.name() + "` precision" );
	}
}
