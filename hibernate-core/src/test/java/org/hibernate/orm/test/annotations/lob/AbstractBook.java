/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.lob;
import org.hibernate.annotations.JavaType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Gail Badner
 */
@MappedSuperclass
public class AbstractBook {
	private String shortDescription;
	private String fullText;
	private Character[] code;
	private char[] code2;
	private Editor editor;

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	@Lob
	@Column(name = "fld_fulltext")
	public String getFullText() {
		return fullText;
	}

	public void setFullText(String fullText) {
		this.fullText = fullText;
	}

	@Lob
	@Column(name = "fld_code")
	@JavaType( CharacterArrayJavaType.class )
	public Character[] getCode() {
		return code;
	}

	public void setCode(Character[] code) {
		this.code = code;
	}

	@Lob
	public char[] getCode2() {
		return code2;
	}

	public void setCode2(char[] code2) {
		this.code2 = code2;
	}

	@Lob
	public Editor getEditor() {
		return editor;
	}

	public void setEditor(Editor editor) {
		this.editor = editor;
	}
}
