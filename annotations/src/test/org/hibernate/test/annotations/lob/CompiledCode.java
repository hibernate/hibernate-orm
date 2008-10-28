//$Id$
package org.hibernate.test.annotations.lob;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * Compiled code representation
 *
 * @author Emmanuel Bernard
 */
@Entity
public class CompiledCode {
	private Integer id;
	private Byte[] header;
	private byte[] fullCode;
	private byte[] metadata;

	public byte[] getMetadata() {
		return metadata;
	}

	public void setMetadata(byte[] metadata) {
		this.metadata = metadata;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
