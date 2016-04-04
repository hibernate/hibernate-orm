create table Contact(
    id integer not null,
    name_firstName VARCHAR,
    name_middleName VARCHAR,
    name_lastName VARCHAR,
    homeAddress_line1 VARCHAR,
    homeAddress_line2 VARCHAR,
    homeAddress_zipCode_postalCode VARCHAR,
    homeAddress_zipCode_plus4 VARCHAR,
    mailingAddress_line1 VARCHAR,
    mailingAddress_line2 VARCHAR,
    mailingAddress_zipCode_postalCode VARCHAR,
    mailingAddress_zipCode_plus4 VARCHAR,
    workAddress_line1 VARCHAR,
    workAddress_line2 VARCHAR,
    workAddress_zipCode_postalCode VARCHAR,
    workAddress_zipCode_plus4 VARCHAR,
    ...
)