/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.format.JsonDocumentWriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;

import static org.hibernate.type.descriptor.jdbc.StructHelper.getSubPart;

/**
 * Stateless helper class to serialize managed type values to JSON.
 */
public class JsonGeneratingVisitor {

	public static final JsonGeneratingVisitor INSTANCE = new JsonGeneratingVisitor();

	protected JsonGeneratingVisitor() {
	}

	/**
	 * Serializes an array of values into JSON object/array
	 *
	 * @param elementJavaType the array element type
	 * @param elementJdbcType the JDBC type
	 * @param values values to be serialized
	 * @param options wrapping options
	 * @param writer the document writer used for serialization
	 */
	public void visitArray(JavaType<?> elementJavaType, JdbcType elementJdbcType, Object[] values, WrapperOptions options, JsonDocumentWriter writer) {
		writer.startArray();
		if ( values.length == 0 ) {
			writer.endArray();
			return;
		}

		if ( elementJdbcType instanceof JsonJdbcType jsonElementJdbcType ) {
			visitPluralAggregateValues( jsonElementJdbcType, values, options, writer );
		}
		else {
			assert !(elementJdbcType instanceof AggregateJdbcType);
			visitBasicPluralValues( elementJavaType, elementJdbcType, values, options, writer );
		}
		writer.endArray();
	}

	private void visitPluralAggregateValues(
			AggregateJdbcType elementJdbcType,
			Object values,
			WrapperOptions options,
			JsonDocumentWriter writer) {
		final EmbeddableMappingType embeddableMappingType = elementJdbcType.getEmbeddableMappingType();
		if ( values.getClass().isArray() ) {
			final var length = Array.getLength( values );
			for ( int j = 0; j < length; j++ ) {
				try {
					visit( embeddableMappingType, Array.get( values, j ), options, writer );
				}
				catch (IOException e) {
					throw new IllegalArgumentException( "Could not serialize array element", e );
				}
			}
		}
		else if ( values instanceof Collection<?> collection ) {
			for ( final var item : collection ) {
				try {
					visit( embeddableMappingType, item, options, writer );
				}
				catch (IOException e) {
					throw new IllegalArgumentException( "Could not serialize array element", e );
				}
			}
		}
		else {
			throw new IllegalArgumentException(
					"Expected array or collection value, but got: " + values.getClass().getName()
			);
		}
	}

	private void visitBasicPluralValues(
			JavaType<?> elementJavaType,
			JdbcType elementJdbcType,
			Object values,
			WrapperOptions options,
			JsonDocumentWriter writer) {
		if ( values.getClass().isArray() ) {
			final var length = Array.getLength( values );
			for ( int j = 0; j < length; j++ ) {
				final var item = Array.get( values, j );
				if ( item == null ) {
					writer.nullValue();
				}
				else {
					writer.serializeJsonValue( item, elementJavaType, elementJdbcType, options );
				}
			}
		}
		else if ( values instanceof Collection<?> collection ) {
			for ( final var item : collection ) {
				if ( item == null ) {
					writer.nullValue();
				}
				else {
					writer.serializeJsonValue( item, elementJavaType, elementJdbcType, options );
				}
			}
		}
		else {
			throw new IllegalArgumentException(
					"Expected array or collection value, but got: " + values.getClass().getName()
			);
		}
	}

	public void visit(MappingType mappedType, Object value, WrapperOptions options, JsonDocumentWriter writer)
			throws IOException {
		if ( handleNullOrLazy( value, writer ) ) {
			// nothing left to do
			return;
		}

		if ( mappedType instanceof EntityMappingType entityType ) {
			serializeEntity( value, entityType, options, writer );
		}
		else if ( mappedType instanceof ManagedMappingType managedMappingType ) {
			serializeObject( managedMappingType, value, options, writer );
		}
		else if ( mappedType instanceof BasicType<?> basicType ) {
			if ( basicType.getJdbcType() instanceof ArrayJdbcType arrayJdbcType ) {
				final var domainValue = basicType.convertToRelationalValue( value );
				final var elementJdbcType = arrayJdbcType.getElementJdbcType();
				writer.startArray();
				if ( elementJdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
					visitPluralAggregateValues(
							aggregateJdbcType,
							domainValue,
							options,
							writer
					);
				}
				else {
					visitBasicPluralValues(
							((BasicPluralJavaType<?>) basicType.getJdbcJavaType()).getElementJavaType(),
							elementJdbcType,
							domainValue,
							options,
							writer
					);
				}
				writer.endArray();
			}
			else {
				writer.serializeJsonValue(
						basicType.convertToRelationalValue( value ),
						basicType.getJdbcJavaType(),
						basicType.getJdbcType(),
						options
				);
			}
		}
		else {
			throw new UnsupportedOperationException(
					"Support for mapping type not yet implemented: " + mappedType.getClass().getName()
			);
		}
	}

	/**
	 * Checks the provided {@code value} is either null or a lazy property.
	 *
	 * @param value the value to check
	 * @param writer the current {@link JsonDocumentWriter}
	 * @return {@code true} if it was, indicating no further processing of the value is needed, {@code false otherwise}.
	 */
	protected boolean handleNullOrLazy(Object value, JsonDocumentWriter writer) {
		if ( value == null ) {
			writer.nullValue();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Serialized an Object value to JSON object using a document writer.
	 *
	 * @param managedMappingType the managed mapping type of the given value
	 * @param value the value to be serialized
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if the underlying writer failed to serialize a mpped value or failed to perform need I/O.
	 */
	private void serializeObject(ManagedMappingType managedMappingType, Object value, WrapperOptions options, JsonDocumentWriter writer)
			throws IOException {
		writer.startObject();
		serializeObjectValues( managedMappingType, value, options, writer );
		writer.endObject();
	}

	/**
	 * JSON object managed type serialization.
	 *
	 * @param managedMappingType the managed mapping type of the given object
	 * @param object the object to be serialized
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if an error occurred while writing to an underlying writer
	 * @see #serializeObject(ManagedMappingType, Object, WrapperOptions, JsonDocumentWriter)
	 */
	protected void serializeObjectValues(ManagedMappingType managedMappingType, Object object, WrapperOptions options, JsonDocumentWriter writer)
			throws IOException {
		final Object[] values = managedMappingType.getValues( object );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart subPart = getSubPart( managedMappingType, i );
			final Object value = values[i];
			serializeModelPart( subPart, value, options, writer );
		}
	}

	protected void serializeModelPart(
			ValuedModelPart modelPart,
			Object value,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		if ( modelPart instanceof SelectableMapping selectableMapping ) {
			writer.objectKey( selectableMapping.getSelectableName() );
			visit( modelPart.getMappedType(), value, options, writer );
		}
		else if ( modelPart instanceof EmbeddedAttributeMapping embeddedAttribute ) {
			if ( value != null ) {
				final EmbeddableMappingType mappingType = embeddedAttribute.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( aggregateMapping == null ) {
					serializeObjectValues( mappingType, value, options, writer );
				}
				else {
					final String name = aggregateMapping.getSelectableName();
					writer.objectKey( name );
					visit( mappingType, value, options, writer );
				}
			}
		}
		else {
			// could not handle model part, throw exception
			throw new UnsupportedOperationException(
					"Support for model part type not yet implemented: "
					+ (modelPart != null ? modelPart.getClass().getName() : "null")
			);
		}
	}

	protected void serializeEntity(
			Object value,
			EntityMappingType entityType,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		// We only need the identifier here
		final EntityIdentifierMapping identifierMapping = entityType.getIdentifierMapping();
		serializeEntityIdentifier( value, identifierMapping, options, writer );
	}

	protected void serializeEntityIdentifier(
			Object value,
			EntityIdentifierMapping identifierMapping,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		final Object identifier = identifierMapping.getIdentifier( value );
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping singleAttribute ) {
			writer.serializeJsonValue(
					identifier,
					singleAttribute.getJavaType(),
					singleAttribute.getSingleJdbcMapping().getJdbcType(),
					options
			);
		}
		else if ( identifier instanceof CompositeIdentifierMapping composite ) {
			visit( composite.getMappedType(), identifier, options, writer );
		}
		else {
			throw new UnsupportedOperationException(
					"Unsupported identifier type: " + identifier.getClass().getName() );
		}
	}
}
