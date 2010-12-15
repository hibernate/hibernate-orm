package org.hibernate.test.annotations.xml.ejb3;

import java.util.HashMap;
import java.util.Map;

//import javax.persistence.CollectionTable;
//import javax.persistence.Column;
//import javax.persistence.ElementCollection;
//import javax.persistence.JoinColumn;
//import javax.persistence.MapKeyClass;
//import javax.persistence.MapKeyColumn;

public class Company {
	int id;
	Map organization = new HashMap();
//	@ElementCollection(targetClass=String.class)
//	@MapKeyClass(String.class)
//	@MapKeyColumn(name="room_number")
//	@Column(name="phone_extension")
//	@CollectionTable(name="phone_extension_lookup", joinColumns={@JoinColumn(name="company_id", referencedColumnName="id")})
	Map conferenceRoomExtensions = new HashMap();
}
