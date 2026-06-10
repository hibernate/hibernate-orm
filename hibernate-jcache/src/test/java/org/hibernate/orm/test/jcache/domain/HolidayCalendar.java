/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;



import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.Table;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "CALENDAR")
public class HolidayCalendar {


	@Id
	@GeneratedValue
	@Column(name = "CALENDAR_ID")
	private Long id;
	private String name;

	// Date -> String
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "CALENDAR_HOLIDAYS", joinColumns = @JoinColumn(name = "CALENDAR_ID"))
	@MapKeyColumn(name = "hol_date")
	@MapKeyTemporal(TemporalType.DATE)
	@Column(name = "hol_name")
	private Map<Date, String> holidays = new HashMap<>();

	public HolidayCalendar init() {
		name = "default";
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
		try {
			holidays.clear();
			holidays.put(df.parse("2009.01.01"), "New Year's Day");
			holidays.put(df.parse("2009.02.14"), "Valentine's Day");
			holidays.put(df.parse("2009.11.11"), "Armistice Day");
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<Date, String> getHolidays() {
		return holidays;
	}

	protected void setHolidays(Map<Date, String> holidays) {
		this.holidays = holidays;
	}

	public void addHoliday(Date d, String name) {
		holidays.put(d, name);
	}

	public String getHoliday(Date d) {
		return (String)holidays.get(d);
	}

	public boolean isHoliday(Date d) {
		return holidays.containsKey(d);
	}

	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}
}
