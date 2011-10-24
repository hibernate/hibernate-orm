package org.hibernate.test.annotations.any;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ManyToAny;

@Entity
@Table( name = "property_list" )
public class PropertyList<T extends Property> {
	private Integer id;

	private String name;

	private T someProperty;

	private List<T> generalProperties = new ArrayList<T>();

	public PropertyList() {
		super();
	}

	public PropertyList(String name) {
		this.name = name;
	}

    @ManyToAny( metaDef = "Property", metaColumn = @Column(name = "property_type") )
    @Cascade( { org.hibernate.annotations.CascadeType.ALL })
    @JoinTable(name = "list_properties",
			joinColumns = @JoinColumn(name = "obj_id"),
			inverseJoinColumns = @JoinColumn(name = "property_id")
	)
    @OrderColumn(name = "prop_index")
    public List<T> getGeneralProperties() {
        return generalProperties;
    }

    public void setGeneralProperties(List<T> generalProperties) {
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

    @Any( metaDef = "Property", metaColumn = @Column(name = "property_type") )
	@Cascade( CascadeType.ALL )
	@JoinColumn(name = "property_id")
	public T getSomeProperty() {
        return someProperty;
    }

	public void setSomeProperty(T someProperty) {
		this.someProperty = someProperty;
	}

	public void addGeneratedProperty(T property) {
		this.generalProperties.add( property );
	}
}
