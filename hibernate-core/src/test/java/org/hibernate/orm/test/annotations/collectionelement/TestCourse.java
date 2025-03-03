/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * @author Emmanuel Bernard
 */
@Entity
@FilterDef(name="selectedLocale", parameters={ @ParamDef( name="param", type=String.class ) } )
public class TestCourse {

	private Long testCourseId;

	private LocalizedString title;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getTestCourseId() {
		return testCourseId;
	}

	public void setTestCourseId(Long testCourseId) {
		this.testCourseId = testCourseId;
	}

	@Embedded
	public LocalizedString getTitle() {
		return title;
	}

	public void setTitle(LocalizedString title) {
		this.title = title;
	}
}
