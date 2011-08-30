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
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

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
