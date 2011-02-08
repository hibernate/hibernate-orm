/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.lob;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

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
