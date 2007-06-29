package org.hibernate.test.legacy;

/**
 * @author hbm2java
 */
public class Person extends org.hibernate.test.legacy.Party {

   java.lang.String id;
   java.lang.String givenName;
   java.lang.String lastName;
   java.lang.String nationalID;


  java.lang.String getId() {
    return id;
  }

  void  setId(java.lang.String newValue) {
    id = newValue;
  }

  java.lang.String getGivenName() {
    return givenName;
  }

  void  setGivenName(java.lang.String newValue) {
    givenName = newValue;
  }

  java.lang.String getLastName() {
    return lastName;
  }

  void  setLastName(java.lang.String newValue) {
    lastName = newValue;
  }

  java.lang.String getNationalID() {
    return nationalID;
  }

  void  setNationalID(java.lang.String newValue) {
    nationalID = newValue;
  }


}
