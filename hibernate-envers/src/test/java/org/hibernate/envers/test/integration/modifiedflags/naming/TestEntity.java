/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags.naming;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited(withModifiedFlag = true)
public class TestEntity {
	@Id
	@GeneratedValue
	private Integer id;
	private String data1;
	@Column(name = "mydata")
	private String data2;
	@Audited(modifiedColumnName = "data_3", withModifiedFlag = true)
	private String data3;
	@Column(name = "thedata")
	@Audited(modifiedColumnName = "the_data_mod", withModifiedFlag = true)
	private String data4;
	@Embedded
	private TestEmbeddable embeddable;
	@ManyToOne
	@JoinColumns({
			@JoinColumn(name = "other_entity_id1", nullable = false),
			@JoinColumn(name = "other_entity_id2", nullable = false)
	})
	private OtherEntity otherEntity;

	@OneToOne
	@JoinColumn(name = "single_id")
	private SingleIdEntity singleIdEntity;

	@OneToOne
	private SingleIdEntity singleIdEntity2;

	@Column(name = "client_option")
	@Enumerated(EnumType.STRING)
	private ClientOption clientOption;

	@Column(name = "client_option2")
	@Enumerated(EnumType.STRING)
	@Audited(withModifiedFlag = true, modifiedColumnName = "cop_mod")
	private ClientOption clientOption2;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	public String getData3() {
		return data3;
	}

	public void setData3(String data3) {
		this.data3 = data3;
	}

	public String getData4() {
		return data4;
	}

	public void setData4(String data4) {
		this.data4 = data4;
	}

	public TestEmbeddable getEmbeddable() {
		return embeddable;
	}

	public void setEmbeddable(TestEmbeddable embeddable) {
		this.embeddable = embeddable;
	}

	public OtherEntity getOtherEntity() {
		return otherEntity;
	}

	public void setOtherEntity(OtherEntity otherEntity) {
		this.otherEntity = otherEntity;
	}

	public SingleIdEntity getSingleIdEntity() {
		return singleIdEntity;
	}

	public void setSingleIdEntity(SingleIdEntity singleIdEntity) {
		this.singleIdEntity = singleIdEntity;
	}

	public SingleIdEntity getSingleIdEntity2() {
		return singleIdEntity2;
	}

	public void setSingleIdEntity2(SingleIdEntity singleIdEntity2) {
		this.singleIdEntity2 = singleIdEntity2;
	}

	public ClientOption getClientOption() {
		return clientOption;
	}

	public void setClientOption(ClientOption clientOption) {
		this.clientOption = clientOption;
	}

	public ClientOption getClientOption2() {
		return clientOption2;
	}

	public void setClientOption2(ClientOption clientOption2) {
		this.clientOption2 = clientOption2;
	}
}
