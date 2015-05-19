/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
