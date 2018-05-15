/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.data;

import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class LobTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Lob
	@Audited
	private String stringLob;

	@Lob
	@Audited
	private byte[] byteLob;

	@Lob
	@Audited
	private char[] charLob;

	@NotAudited
	private String data;

	public LobTestEntity() {
	}

	public LobTestEntity(String stringLob, byte[] byteLob, char[] charLob) {
		this.stringLob = stringLob;
		this.byteLob = byteLob;
		this.charLob = charLob;
	}

	public LobTestEntity(Integer id, String stringLob, byte[] byteLob, char[] charLob) {
		this.id = id;
		this.stringLob = stringLob;
		this.byteLob = byteLob;
		this.charLob = charLob;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStringLob() {
		return stringLob;
	}

	public void setStringLob(String stringLob) {
		this.stringLob = stringLob;
	}

	public byte[] getByteLob() {
		return byteLob;
	}

	public void setByteLob(byte[] byteLob) {
		this.byteLob = byteLob;
	}

	public char[] getCharLob() {
		return charLob;
	}

	public void setCharLob(char[] charLob) {
		this.charLob = charLob;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof LobTestEntity) ) {
			return false;
		}

		LobTestEntity that = (LobTestEntity) o;

		if ( !Arrays.equals( byteLob, that.byteLob ) ) {
			return false;
		}
		if ( !Arrays.equals( charLob, that.charLob ) ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( stringLob != null ? !stringLob.equals( that.stringLob ) : that.stringLob != null ) {
			return false;
		}
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (stringLob != null ? stringLob.hashCode() : 0);
		result = 31 * result + (byteLob != null ? Arrays.hashCode( byteLob ) : 0);
		result = 31 * result + (charLob != null ? Arrays.hashCode( charLob ) : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}
}