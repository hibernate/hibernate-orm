INSERT INTO integer_property
       ( "name", "value", id )
VALUES ( 'age', 23, 1 )

INSERT INTO string_property
       ( "name", "value", id )
VALUES ( 'name', 'John Doe', 1 )

INSERT INTO property_repository ( id )
VALUES ( 1 )

INSERT INTO repository_properties
    ( repository_id , property_type , property_id )
VALUES
    ( 1 , 'I' , 1 )