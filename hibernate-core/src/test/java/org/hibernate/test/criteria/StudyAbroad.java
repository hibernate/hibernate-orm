package org.hibernate.test.criteria;

import java.util.Date;

public class StudyAbroad {
    private Country country;
    private Date date;
    
    public StudyAbroad() {}
    
    public StudyAbroad(Country country, Date date) {
	this.country = country;
	this.date = date;
    }
    
    public Country getCountry() {
        return country;
    }
    
    public void setCountry(Country country) {
        this.country = country;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
}
