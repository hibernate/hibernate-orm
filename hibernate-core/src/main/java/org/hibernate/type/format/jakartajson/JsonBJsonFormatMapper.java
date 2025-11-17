/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jakartajson;

import org.hibernate.type.format.AbstractJsonFormatMapper;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.lang.reflect.Type;

/**
 * @author Christian Beikov
 * @author Yanming Zhou
 * @author Emmanuel Jannetti
 */
public final class JsonBJsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jsonb";

	private final Jsonb jsonb;

	public JsonBJsonFormatMapper() {
		this( JsonbBuilder.create() );
	}

	public JsonBJsonFormatMapper(Jsonb jsonb) {
		this.jsonb = jsonb;
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
