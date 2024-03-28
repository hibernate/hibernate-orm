/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jackson;

import org.hibernate.type.format.AbstractJsonFormatMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

/**
 * @author Christian Beikov
 * @author Yanming Zhou
 */
public final class JacksonJsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;

	public JacksonJsonFormatMapper() {
		this(new ObjectMapper().findAndRegisterModules());
	}

	public JacksonJsonFormatMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, Type type) {
		try {
			return objectMapper.readValue( charSequence.toString(), objectMapper.constructType( type ) );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + type, e );
		}
	}

	@Override
	public <T> String toString(T value, Type type) {
		try {
			return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + type, e );
		}
	}
}
