# APPOINTMENT BOOKING SYSTEM PROJECT

## Scope
- User management (clients & managers)
- Appointment management

## Static Data (stored in DB or not, at your discretion)
- Time slots: list of hours from 08:00 to 16:00; each slot lasts one hour
- Services: list of 5 services: Archives; Finance Department (DAF); Human Resources (HR); Accounting; Social Affairs

## Specifications
- Each appointment has one and only one manager
- A manager cannot have more than one appointment per time slot
- An appointment can include a maximum of two individuals
- An appointment must be scheduled at least two days in advance

**Note:** All operations must be exposed via REST APIs. No UI or frontend is required.

## Input JSON Objects

### Minimal Appointment Object
```json
{
  "refClient": "string",
  "refRDV": "string",
  "refService": "string",
  "refResponsable": "string",
  "dateRDV": "date & time",
  "motifRdv": "Appointment reason"
}
```

### Minimal User Object
```json
{
  "ref": "string",
  "email": "string",
  "telephone": "int",
  "nom": "string",
  "prenom": "string"
}
```

## Technical Constraints & Expectations

1. Language/Framework  
   - Java 21, Spring Boot 3.x  
   - Use modern Java features where appropriate (e.g., Records, Switch Expressions)

2. Database  
   - PostgreSQL  

3. Data Modeling  
   - Design a normalized relational schema  
   - Provide DDL (or Flyway/Liquibase scripts) along with the code  

4. Concurrency  
   - Handle potential race conditions  
   - Ensure consistency when multiple appointments are booked simultaneously  
   - Use appropriate locking or optimistic concurrency control  

5. Error Handling  
   - Implement robust and standardized API error responses  
   - (e.g., using @ControllerAdvice)

6. Testing  
   - Include relevant unit tests (JUnit 5, Mockito)  
   - Include at least one integration test covering transaction processing flow  

7. Submission  
   - Code must be hosted on a public GitHub repository  
   - Include a README.md explaining how to run the application and execute tests  

## Evaluation Criteria

- Code Structure & Cleanliness  
  - Adherence to SOLID principles  
  - Proper use of Spring conventions  

- Data Integrity & Consistency  
  - Proper transaction boundaries  
  - Effective concurrency management  

- Test Quality  
  - Test effectiveness (not just coverage percentage)  

- Modern Java Usage  
  - Effective use of Java 21 features  
