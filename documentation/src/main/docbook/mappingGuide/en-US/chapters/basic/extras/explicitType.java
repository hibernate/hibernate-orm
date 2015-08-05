@org.hibernate.annotations.Type( type="nstring" )
private String name;

@org.hibernate.annotations.Type( type="materialized_nclob" )
private String description;