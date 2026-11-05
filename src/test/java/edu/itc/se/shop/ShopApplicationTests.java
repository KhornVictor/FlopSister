package edu.itc.se.shop;                             
                                                         
import org.junit.jupiter.api.Test;                   
import org.springframework.beans.factory.annotation.Autowired;                                                   
import                                               
org.springframework.boot.test.context.SpringBootTest;
														
import javax.sql.DataSource;                         
import java.sql.Connection;                          
import java.sql.SQLException;                        
														
import static                                        
org.assertj.core.api.Assertions.assertThat;          
														
@SpringBootTest                                      
class DatabaseConnectionTest {                       
														
	@Autowired                                       
	private DataSource dataSource;                   
														
	@Test                                            
	void testDatabaseConnection() throws SQLException
	{                                                
		assertThat(dataSource).isNotNull();          
														
		try (Connection connection =                 
		dataSource.getConnection()) {                
			assertThat(connection.isValid(1)).isTrue(
			);                                       
			System.out.println("==========================================");                 
			System.out.println("Connected to: " +    
			connection.getMetaData().getDatabaseProductName());                               
			System.out.println("Database URL: " +    
			connection.getMetaData().getURL());      
			System.out.println("User Name:    " +    
			connection.getMetaData().getUserName()); 
			System.out.println("==========================================");                 
		}                                            
	}                                                
}                            