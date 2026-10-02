To run this example:
  - Have [Apache Maven](https://maven.apache.org) installed
  - Have [H2 Sakila database](https://github.com/hibernate/sakila-h2) running
  - Issue the following command from a command-line window opened in this folder:
```shell
mvn generate-sources 
  -Dh2.version=${h2.version} 
  -Dhibernate.version=${hibernate.version}  
  -Doutput.dir=./generated-classes
```
    