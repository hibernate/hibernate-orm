/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.format.JsonDocumentHandler;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Implementation of <code>JsonDocumentHandler</code> for OSON document.
 * This implementation will produce an Object Array based on
 * embeddable mapping
 *
 */
public class ObjectArrayOsonDocumentHandler implements JsonDocumentHandler {

	private Object [] objectArrayResult;
	SelectableMapping mapping = null;
	String currentKeyName = null;
	List<Object> subArrayObjectList = null;
	BasicPluralType<?, ?> subArrayObjectTypes = null;

	// mapping definitions are in a tree
	// Each mapping definition may contain sub mappings (sub embeddable mapping)
	// This stack is used to keep pointer on mapping to be used
	// see startObject/endObject methods
	Stack<EmbeddableMappingType> embeddableMappingTypes = new Stack<>();

	WrapperOptions wrapperOptions;

	// index within objectArrayResult
	int currentSelectableIndexInResultArray = -1;

	public ObjectArrayOsonDocumentHandler(EmbeddableMappingType embeddableMappingType, WrapperOptions wrapperOptions) {
		this.embeddableMappingTypes.push(embeddableMappingType);
		this.wrapperOptions = wrapperOptions;
		this.objectArrayResult = new Object[embeddableMappingType.getJdbcValueCount()];
	}

	/**
	 * Gets the Object array built from document handling
	 * @return the array
	 */
	public Object [] getMappedObjectArray() {
		return this.objectArrayResult;
	}

	@Override
	public void startObject() {
		if (currentKeyName != null) {
			// we are dealing with a sub-object, allocate space for it.
			// otherwise, we have nothing to do.
			currentSelectableIndexInResultArray = embeddableMappingTypes.peek().getSelectableIndex( currentKeyName );
			assert currentSelectableIndexInResultArray != -1: "Cannot get index of " + currentKeyName;

			final SelectableMapping selectable = embeddableMappingTypes.peek().getJdbcValueSelectable(
					currentSelectableIndexInResultArray );
			final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping()
					.getJdbcType();
			final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
			objectArrayResult[currentSelectableIndexInResultArray] =
					new Object[subMappingType.getJdbcValueCount()];
			embeddableMappingTypes.push( subMappingType );
		}
	}

	@Override
	public void endObject() {
		embeddableMappingTypes.pop();
	}

	@Override
	public void startArray() {
		assert (subArrayObjectList == null && subArrayObjectTypes == null) : "startArray called twice ?";

		// initialize an array to gather values
		subArrayObjectList = new ArrayList<>();
		assert (mapping.getJdbcMapping() instanceof BasicPluralType<?, ?>)
				: "Array event received for non plural type";
		// initialize array's element type
		subArrayObjectTypes = (BasicPluralType<?, ?>) mapping.getJdbcMapping();
	}

	@Override
	public void endArray() {
		assert (subArrayObjectList != null && subArrayObjectTypes != null) : "endArray called before startArray";
		// flush array values
		objectArrayResult[currentSelectableIndexInResultArray] = subArrayObjectTypes.getJdbcJavaType().wrap( subArrayObjectList, wrapperOptions );
		// reset until we encounter next array element
		subArrayObjectList = null;
		subArrayObjectTypes = null;
	}

	@Override
	public void onObjectKey(String key) {
		this.currentKeyName = key;

		currentSelectableIndexInResultArray = embeddableMappingTypes.peek().getSelectableIndex( currentKeyName );
		if ( currentSelectableIndexInResultArray >= 0 ) {
			// we may not have a selectable mapping for that key
			mapping = embeddableMappingTypes.peek().getJdbcValueSelectable( currentSelectableIndexInResultArray );
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Could not find selectable [%s] in embeddable type [%s] for JSON processing.",
							currentKeyName,
							embeddableMappingTypes.peek().getMappedJavaType().getJavaTypeClass().getName()
					)
			);
		}
	}

	@Override
	public void onNullValue() {
		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( null );
		}
		else {
			objectArrayResult[currentSelectableIndexInResultArray] = null;
		}
	}

	@Override
	public void onBooleanValue(boolean value) {
		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( value?Boolean.TRUE:Boolean.FALSE);
		}
		else {
			objectArrayResult[currentSelectableIndexInResultArray] = value?Boolean.TRUE:Boolean.FALSE;
		}
	}

	@Override
	public void onStringValue(String value) {
		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add(
					subArrayObjectTypes.getElementType().getJdbcJavaType().fromString( value ) );
		}
		else {
			objectArrayResult[currentSelectableIndexInResultArray] =
					mapping.getJdbcMapping().getJdbcJavaType().fromString( value);
		}
	}

	/**
	 * Callback for OSON values
	 * @param value the OSON value
	 * @param <T> the type of the value as returned by OracleJsonParser
	 */
	public <T> void onOsonValue(T value) {
		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( value );
		}
		else {
			objectArrayResult[currentSelectableIndexInResultArray] =
					mapping.getJdbcMapping().convertToDomainValue(
					mapping.getJdbcMapping().getJdbcJavaType()
							.wrap( value, wrapperOptions ) );
		}
	}

	/**
	 * Callback for OSON binary value
	 * @param bytes the OSON byters
	 */
	public void onOsonBinaryValue(byte[] bytes) {
		Class underlyingType = null;
		Object theOneToBeUsed;
		if(subArrayObjectTypes!=null) {
			underlyingType = subArrayObjectTypes.getElementType().getJavaType();
		}
		else {
			underlyingType = (Class) mapping.getJdbcMapping().getJdbcJavaType().getJavaType();
		}

		if (java.util.UUID.class.isAssignableFrom( underlyingType ))  {
			theOneToBeUsed = UUIDJavaType.INSTANCE.wrap( bytes, wrapperOptions );
		}
		else {
			theOneToBeUsed = bytes;
		}

		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( theOneToBeUsed );
		}
		else {
			objectArrayResult[currentSelectableIndexInResultArray] = theOneToBeUsed;
		}
	}

	/**
	 * Callback for OracleJsonParser.Event.VALUE_DATE and OracleJsonParser.Event.VALUE_TIMESTAMP:
	 * @param localDateTime the time
	 */
	public void onOsonDateValue(LocalDateTime localDateTime) {

		Class underlyingType = null;
		Object theOneToBeUsed = localDateTime;

		if(subArrayObjectTypes!=null) {
			underlyingType = subArrayObjectTypes.getElementType().getJavaType();
		}
		else {
			underlyingType = (Class) mapping.getJdbcMapping().getJdbcJavaType().getJavaType();
		}
		if (java.sql.Date.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = Date.valueOf( localDateTime.toLocalDate());
		}
		else if (java.time.LocalDate.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = localDateTime.toLocalDate();
		}
		else if (java.sql.Timestamp.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = Timestamp.valueOf( localDateTime );
		}

		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( theOneToBeUsed );
		}
		else {
			objectArrayResult[currentSelectableIndexInResultArray] = theOneToBeUsed;
		}
	}
}
