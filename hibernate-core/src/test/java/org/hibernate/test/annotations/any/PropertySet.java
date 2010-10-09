package org.hibernate.test.annotations.any;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

@Entity
@Table( name = "property_set" )
public class PropertySet {
	private Integer id;
	private String name;
	private Property someProperty;

	private List<Property> generalProperties = new ArrayList<Property>();

	public PropertySet() {
		super();
	}

	public PropertySet(String name) {
		this.name = name;
	}

	@ManyToAny(
			metaColumn = @Column( name = "property_type" ) )
	@AnyMetaDef( idType = "integer", metaType = "string",
			metaValues = {
			@MetaValue( value = "S", targetEntity = StringProperty.class ),
			@MetaValue( value = "I", targetEntity = IntegerProperty.class ) } )
	@Cascade( { org.hibernate.annotations.CascadeType.ALL } )
	@JoinTable( name = "obj_properties", joinColumns = @JoinColumn( name = "obj_id" ),
			inverseJoinColumns = @JoinColumn( name = "property_id" ) )
	public List<Property> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(List<Property> generalProperties) {
		this.generalProperties = generalProperties;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Any( metaColumn = @Column( name = "property_type" ) )
	@Cascade( value = { CascadeType.ALL } )
	@AnyMetaDef( idType = "integer", metaType = "string", metaValues = {
	@MetaValue( value = "S", targetEntity = StringProperty.class ),
	@MetaValue( value = "I", targetEntity = IntegerProperty.class )
			} )
	@JoinColumn( name = "property_id" )
	public Property getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(Property someProperty) {
		this.someProperty = someProperty;
	}

	public void addGeneratedProperty(Property property) {
		this.generalProperties.add( property );
	}
}
