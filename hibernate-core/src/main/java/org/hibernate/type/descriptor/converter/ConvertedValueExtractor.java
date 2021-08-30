/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ConvertedValueExtractor<O,R> implements ValueExtractor<O> {
	private static final Logger log = Logger.getLogger( ConvertedValueExtractor.class );

	private final ValueExtractor<R> relationalExtractor;
	private final BasicValueConverter<O,R> converter;

	public ConvertedValueExtractor(
			ValueExtractor<R> relationalExtractor,
			BasicValueConverter<O, R> converter) {
		this.relationalExtractor = relationalExtractor;
		this.converter = converter;
	}

	@Override
	public O extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		return doConversion( relationalExtractor.extract( rs, paramIndex, options ) );
	}

	@Override
	public O extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
		return doConversion( relationalExtractor.extract( statement, paramIndex, options ) );
	}

	@Override
	public O extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
		return doConversion( relationalExtractor.extract( statement, paramName, options ) );
	}

	private O doConversion(R extractedValue) {
		try {
			O convertedValue = converter.toDomainValue( extractedValue );
			log.debugf( "Converted value on extraction: %s -> %s", extractedValue, convertedValue );
			return convertedValue;
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (RuntimeException re) {
			throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
		}
	}
}
