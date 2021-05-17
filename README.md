# NetSuite-Middleware

Java Spring Boot App which calls e-Invoice APIs to generate token and do some operations on invoices.

## Implemented e-Invoice APIs
- Generate Token
- Submit Document API.


## API Headers & Body

```bash
Header: 
- client_id.
- client_secret.

Body:
- Document Json Object.
```

## Authentication
- Using Basic Auth.

## Helpers
- Request attached in the repo to be imported in Postman and test the API.
- Encryption Console App to encrypt Auth. passwords.
