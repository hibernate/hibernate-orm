package org.hibernate.test.instrument.domain;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Problematic {
	private Long id;
	private String name;
	private byte[] bytes;

	private Representation representation;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public Representation getRepresentation() {
		if ( representation == null ) {
			representation =  ( ( bytes == null ) ? null : new Representation( bytes ) );
		}
		return representation;
	}

	public void setRepresentation(Representation rep) {
		bytes = rep.getBytes();
	}

	public static class Representation {
		private byte[] bytes;

		public Representation(byte[] bytes) {
			this.bytes = bytes;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public String toString() {
			String result = "";
			for ( int i = 0; i < bytes.length; i++ ) {
				result += bytes[i];
			}
			return result;
		}
	}
}
