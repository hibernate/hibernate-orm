/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Implementation of <code>JsonDocumentHandler</code> for OSON document.
 * This implementation will produce an Object Array based on
 * embeddable mapping
 * Once All JSON document is handle the mapped Object array can be retrieved using the
 * <code>getObjectArray()</code> method.
 *
 */
public class ObjectArrayOsonDocumentHandler implements JsonDocumentHandler {

	// final result of a mapped object array
	private final Object [] objectArrayResult;
	// current mapping to be used
	SelectableMapping currentSelectableMapping = null;
	String currentKeyName = null;
	List<Object> subArrayObjectList = null;
	BasicPluralType<?, ?> subArrayObjectTypes = null;

	// mapping definitions are in a tree
	// Each mapping definition may contain sub mappings (sub embeddable mapping)
	// This stack is used to keep a pointer on the current mapping to be used to assign correct types.
	// see onStartObject()/onEndObject() methods
	Stack<EmbeddableMappingType> embeddableMappingTypes = new Stack<>();
	// As for mapping definitions, when "sub embeddable" is encountered, the array
	// that needs to be filled with Objects is the one we allocate in the final result array slot.
	// We use a stack to keep track of array ref
	Stack<Object[]> objectArrays = new Stack<>();


	WrapperOptions wrapperOptions;

	// index within objectArrayResult
	int currentSelectableIndexInResultArray = -1;

	public ObjectArrayOsonDocumentHandler(EmbeddableMappingType embeddableMappingType, WrapperOptions wrapperOptions) {
		this.embeddableMappingTypes.push(embeddableMappingType);
		this.wrapperOptions = wrapperOptions;
		this.objectArrayResult = new Object[embeddableMappingType.getJdbcValueCount()+ ( embeddableMappingType.isPolymorphic() ? 1 : 0 )];
		this.objectArrays.push( this.objectArrayResult );
	}

	/**
	 * Gets the Object array built from document handling
	 * @return the array of objects
	 */
	public Object [] getObjectArray() {
		return this.objectArrayResult;
	}

	@Override
	public void onStartObject() {
		if (currentKeyName != null) {
			// We are dealing with a sub-object, allocate space for it then,
			// otherwise, we have nothing to do.
			// Push the new (sub)mapping definition.
			this.currentSelectableIndexInResultArray = embeddableMappingTypes.peek().getSelectableIndex( currentKeyName );
			assert currentSelectableIndexInResultArray != -1: "Cannot get index of " + currentKeyName;

			final SelectableMapping selectable = embeddableMappingTypes.peek().getJdbcValueSelectable(
					currentSelectableIndexInResultArray );
			final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping()
					.getJdbcType();
			final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
			this.objectArrays.peek()[currentSelectableIndexInResultArray] =
					new Object[subMappingType.getJdbcValueCount()];
			this.embeddableMappingTypes.push( subMappingType );
			this.objectArrays.push( (Object[]) this.objectArrays.peek()[currentSelectableIndexInResultArray] );
		}
	}

	@Override
	public void onEndObject() {
		// go back in the mapping definition tree
		this.embeddableMappingTypes.pop();
		this.objectArrays.pop();
	}

	@Override
	public void onStartArray() {
		assert (subArrayObjectList == null && subArrayObjectTypes == null) : "onStartArray called twice ?";

		// initialize an array to gather values
		subArrayObjectList = new ArrayList<>();
		assert (currentSelectableMapping.getJdbcMapping() instanceof BasicPluralType<?, ?>)
				: "Array event received for non plural type";
		// initialize array's element type
		subArrayObjectTypes = (BasicPluralType<?, ?>) currentSelectableMapping.getJdbcMapping();
	}

	@Override
	public void onEndArray() {
		assert (subArrayObjectList != null && subArrayObjectTypes != null) : "onEndArray called before onStartArray";
		// flush array values
		this.objectArrays.peek()[currentSelectableIndexInResultArray] = subArrayObjectTypes.getJdbcJavaType().wrap( subArrayObjectList, wrapperOptions );
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
			currentSelectableMapping = embeddableMappingTypes.peek().getJdbcValueSelectable( currentSelectableIndexInResultArray );
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
			this.objectArrays.peek()[currentSelectableIndexInResultArray] = null;
		}
	}

	@Override
	public void onBooleanValue(boolean value) {
		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( value?Boolean.TRUE:Boolean.FALSE);
		}
		else {
			this.objectArrays.peek()[currentSelectableIndexInResultArray] = value?Boolean.TRUE:Boolean.FALSE;
		}
	}

	@Override
	public void onStringValue(String value) {
		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add(
					subArrayObjectTypes.getElementType().getJdbcJavaType().fromEncodedString( value ,0,value.length()) );
		}
		else {
			this.objectArrays.peek()[currentSelectableIndexInResultArray] =
					currentSelectableMapping.getJdbcMapping().getJdbcJavaType().fromEncodedString( value,0,value.length());
		}
	}

	@Override
	public void onNumberValue(Number value) {
		onOsonValue(value);
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
			this.objectArrays.peek()[currentSelectableIndexInResultArray] =
					currentSelectableMapping.getJdbcMapping().convertToDomainValue(
					currentSelectableMapping.getJdbcMapping().getJdbcJavaType()
							.wrap( value, wrapperOptions ) );
		}
	}

	/**
	 * Callback for OSON binary value
	 * @param bytes the OSON byters
	 */
	public void onOsonBinaryValue(byte[] bytes) {
		Class underlyingType;
		Object theOneToBeUsed;
		if(subArrayObjectTypes!=null) {
			underlyingType = subArrayObjectTypes.getElementType().getJavaType();
		}
		else {
			underlyingType = (Class) currentSelectableMapping.getJdbcMapping().getJdbcJavaType().getJavaType();
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
			this.objectArrays.peek()[currentSelectableIndexInResultArray] = theOneToBeUsed;
		}
	}

	/**
	 * Callback for OracleJsonParser.Event.VALUE_DATE and OracleJsonParser.Event.VALUE_TIMESTAMP:
	 * @param localDateTime the time
	 */
	public void onOsonDateValue(LocalDateTime localDateTime) {

		Class underlyingType;
		Object theOneToBeUsed = localDateTime;

		if(subArrayObjectTypes!=null) {
			underlyingType = subArrayObjectTypes.getElementType().getJavaType();
		}
		else {
			underlyingType = (Class) currentSelectableMapping.getJdbcMapping().getJdbcJavaType().getJavaType();
		}
		if (java.sql.Date.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = Date.valueOf( localDateTime.toLocalDate());
		}
		else if (java.time.LocalDate.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = localDateTime.toLocalDate();
		}
		else if (java.time.LocalTime.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = localDateTime.toLocalTime();
		}
		else if (java.sql.Time.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = Time.valueOf( localDateTime.toLocalTime() );
		}
		else if (java.sql.Timestamp.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = Timestamp.valueOf( localDateTime );
		}
		else if(java.time.LocalTime.class.isAssignableFrom( underlyingType )) {
			theOneToBeUsed = localDateTime.toLocalTime();
		}
		else if ( java.util.Date.class.isAssignableFrom( underlyingType ) ) {
			// better way?
			theOneToBeUsed = java.util.Date.from( localDateTime.atZone( ZoneId.of( "UTC" ) ).toInstant());
		}

		if ( subArrayObjectList != null ) {
			// dealing with arrays
			subArrayObjectList.add( theOneToBeUsed );
		}
		else {
			this.objectArrays.peek()[currentSelectableIndexInResultArray] = theOneToBeUsed;
		}
	}
}
