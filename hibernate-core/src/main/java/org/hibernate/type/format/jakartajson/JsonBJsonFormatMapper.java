package org.hibernate.type.format.jakartajson;

import org.hibernate.SPI;

import org.hibernate.type.format.AbstractJsonFormatMapper;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.spi.JsonbProvider;

import org.jboss.logging.Logger;

import java.lang.reflect.Type;

/**
 * @author Christian Beikov
 * @author Yanming Zhou
 * @author Emmanuel Jannetti
 */
@SPI
public final class JsonBJsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jsonb";

	private static final Logger LOG = Logger.getLogger( JsonBJsonFormatMapper.class );

	private final Jsonb jsonb;

	public JsonBJsonFormatMapper() {
		this( JsonbBuilder.create() );
	}

	public JsonBJsonFormatMapper(Jsonb jsonb) {
		this.jsonb = jsonb;
	}

	static JsonBJsonFormatMapper createIfAvailable() {
		final JsonbProvider provider;
		try {
			provider = JsonbProvider.provider();
		}
		catch (JsonbException e) {
			LOG.debug( "Unable to discover a JSON-B provider", e );
			return null;
		}
		// Do not ignore failures in an installed provider.
		return new JsonBJsonFormatMapper( provider.create().build() );
	}

	@Override
	public <T> T fromString(CharSequence charSequence, Type type) {
		try {
			return jsonb.fromJson( charSequence.toString(), type );
		}
		catch (JsonbException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + type, e );
		}
	}

	@Override
	public <T> String toString(T value, Type type) {
		try {
			return jsonb.toJson( value, type );
		}
		catch (JsonbException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + type, e );
		}
	}
}
