/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.misc;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@DomainModel(annotatedClasses = {Misc4Test.A.class, Misc4Test.B.class})
@SessionFactory
@JiraKey(value = "HHH-13054")
public class Misc4Test {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction(x -> {});
	}

	@Entity
	@Table(name = "A", schema = "", catalog = "")
	@IdClass(APK.class)
	public static class A {

	private String a1;
	private String a2;
	private String a3;
	private String a4;
	private String a5;
	private String a6;
	private List<B> bObj;

	@Id
	@Column(name = "A1", nullable = false, length = 15)
	public String getA1() {
		return a1;
	}

	public void setA1(String a1) {
		this.a1 = a1;
	}

	@Basic
	@Column(name = "A2", nullable = false, length = 15)
	public String getA2() {
		return a2;
	}

	public void setA2(String a2) {
		this.a2 = a2;
	}

	@Basic
	@Column(name = "A3", nullable = false, length = 15)
	public String getA3() {
		return a3;
	}

	public void setA3(String a3) {
		this.a3 = a3;
	}


	@Id
	@Column(name = "A4", nullable = false, length = 15)
	public String getA4() {
		return a4;
	}

	public void setA4(String a4) {
		this.a4 = a4;
	}


	@Id
	@Column(name = "A5", nullable = false, length = 15)
	public String getA5() {
		return a5;
	}

	public void setA5(String a5) {
		this.a5 = a5;
	}

	@Id
	@Column(name = "A6", nullable = false, length = 15)
	public String getA6() {
		return a6;
	}

	public void setA6(String a6) {
		this.a6 = a6;
	}

	@OneToMany(mappedBy = "aObj")
	public List<B> getB() {
		return bObj;
	}

	public void setB(List<B> bObj) {
		this.bObj = bObj;
	}
	}

	public static class APK implements Serializable {

	private String a1;
	private String a4;
	private String a5;
	private String a6;

	@Column(name = "A1", nullable = false, length = 15)
	@Id
	public String getA1() {
		return a1;
	}

	public void setA1(String a1) {
		this.a1 = a1;
	}

	@Column(name = "A4", nullable = false, length = 15)
	@Id
	public String getA4() {
		return a4;
	}

	public void setA4(String a4) {
		this.a4 = a4;
	}

	@Column(name = "A5", nullable = false, length = 15)
	@Id
	public String getA5() {
		return a5;
	}

	public void setA5(String a5) {
		this.a5 = a5;
	}

	public String getA6() {
		return a6;
	}

	public void setA6(String a6) {
		this.a6 = a6;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
		return true;
		}
		if (o == null || getClass() != o.getClass()) {
		return false;
		}
		APK apk = (APK) o;
		return Objects.equals(a1, apk.a1) &&
			Objects.equals(a4, apk.a4) &&
			Objects.equals(a5, apk.a5) &&
			Objects.equals(a6, apk.a6);
	}

	@Override
	public int hashCode() {

		return Objects.hash(a1, a4, a5, a6);
	}
	}

	@Entity
	@Table(name = "B", schema = "", catalog = "")
	@IdClass(BPK.class)
	public static class B {

	private int a1;
	private String a2;
	private String a3;
	private String b1;
	private String b2;
	private A aObj;

	@Id
	@Column(name = "A1", nullable = false, length = 15)
	public int getA1() {
		return a1;
	}

	public void setA1(int a1) {
		this.a1 = a1;
	}

	@Id
	@Column(name = "A2", nullable = false, length = 15)
	public String getA2() {
		return a2;
	}

	public void setA2(String a2) {
		this.a2 = a2;
	}

	@Id
	@Column(name = "A3", nullable = false, length = 15)
	public String getA3() {
		return a3;
	}

	public void setA3(String a3) {
		this.a3 = a3;
	}

	@Basic
	@Column(name = "B1", nullable = false, length = 15)
	public String getB1() {
		return b1;
	}

	public void setB1(String b1) {
		this.b1 = b1;
	}

	@Basic
	@Column(name = "B2", nullable = false, length = 15)
	public String getB2() {
		return b2;
	}

	public void setB2(String b2) {
		this.b2 = b2;
	}


	@ManyToOne(targetEntity = A.class)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name ="A1", referencedColumnName = "A1" , insertable = false, updatable = false)
	public A getaObj() {
		return aObj;
	}

	public void setaObj(A aObj) {
		this.aObj = aObj;
	}
	}

	public static class BPK implements Serializable {

	private int a1;
	private String a2;
	private String a3;

	@Column(name = "A1", nullable = false, length = 15)
	@Id
	public int getA1() {
		return a1;
	}

	public void setA1(int a1) {
		this.a1 = a1;
	}

	@Column(name = "A2", nullable = false, length = 15)
	@Id
	public String getA2() {
		return a2;
	}

	public void setA2(String a2) {
		this.a2 = a2;
	}

	@Column(name = "A3", nullable = false, length = 15)
	@Id
	public String getA3() {
		return a3;
	}

	public void setA3(String a3) {
		this.a3 = a3;
	}
	}
}
