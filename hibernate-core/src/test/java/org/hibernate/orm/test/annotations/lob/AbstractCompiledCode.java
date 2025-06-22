/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;
import org.hibernate.annotations.JavaType;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;

import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Gail Badner
 */
@MappedSuperclass
public class AbstractCompiledCode {
	private Byte[] header;
	private byte[] fullCode;
	private byte[] metadata;

	public byte[] getMetadata() {
		return metadata;
	}

	public void setMetadata(byte[] metadata) {
		this.metadata = metadata;
	}

	@Lob
	@JavaType( ByteArrayJavaType.class )
	public Byte[] getHeader() {
		return header;
	}

	public void setHeader(Byte[] header) {
		this.header = header;
	}

	@Lob
	public byte[] getFullCode() {
		return fullCode;
	}

	public void setFullCode(byte[] fullCode) {
		this.fullCode = fullCode;
	}
}
